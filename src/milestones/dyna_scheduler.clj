;;    <The Dynamic Scheduler - Part of Automagic Tools / Milestones>
;;    Copyright (C) 2014 , Rafik NACCACHE <rafik@automagic.tools>

;; This program is free software: you can redistribute it and/or
;; modify it under the terms of the GNU General Public License as
;; published by the Free Software Foundation, either version 3 of
;; the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public
;; License along with this program.  If not, see
;; <http://www.gnu.org/licenses/>.

;;; Using core.async channels, it will plan tasks.
;;; input is a map of tasks like:


;;;           1 {:task-name "A description about this task"
;;;                   ;the resource that'll be booked doing the task
;;;            :resource-id 3
;;;                    ;the duration that this resource will be booked during this task
;;;            :duration 3
;;;                     ;priority : less is higher priority
;;;            :priority 1
;;;                     ;predecessors : if they are not complete, task cannot be fired.
;;;             predecessors [2 4]}


;;; work queue are the tasks to be processed, i.e [ 2 3 1...
;;; work flow are the task units to be fed to the channel , i.e [ 1 1 1 3 3 3...
;;; work in progress are the task that have already been processed by the resource
;;; complete task is a task already totally in the output schedule.
;;; an output schedule is like this :
;;; [{:task-id 1 :time 1 :resource-id 1}
;;; {:task-id 1 :time 2 :resource-id 1}
;;; {:task-id 3 :time 1 :resource-id 1}
;;; {:task-id 3 :time 2 :resource-id 1}
;;; {:task-id 3 :time 3 :resource-id 1}]

;;; a function exist in the end that emits same tasks with begin field, or errors to be treated.

(ns milestones.dyna-scheduler
  (:require
    [milestones.graph-utilities :refer :all]
    [clojure.set]
    [clojure.core.async :as async
     :refer [chan go alts! alts!! >! >!! <!! <! close! timeout]]))

(defn gen-work-flow
  "Given all tasks description vector [{:task-id, ...},{}]
  and a work-queue [1 2 3],... we generate named task units
  with as many unit of each task as its duration :
  [1 1 1 2 2 3 3 3]"
  [tasks
   work-queue]
  (->> work-queue
       (mapcat  #(let [the-id %
                       the-task (get tasks %) ]
                   (repeat (:duration the-task) the-id)))
       (vec)))

(defn task-completion-rate
  "Given tasks description, a schedule-output
  [{:task-id 1 :resource-id 1 :time 2}
  {:task-id 1 :resource-id 1 :time 2} ...]
  and a task-id, returns the completion-rate,i.e,
  nb of units in output / duration of task. if no task in the schedule,
  it's completion is 0. If no duration / 0, completion is 1. "
  [tasks
   output-schedule
   the-task-id]
  (try
    (let [the-task (get tasks the-task-id)
          duration (the-task :duration)
          nb-task-units-in-output (count
                                    (filter #(= (% :task-id ) the-task-id )
                                            output-schedule ))]
      (/ nb-task-units-in-output duration))
    (catch Exception e 1)))

(defn task-complete?
  "Returns true if task is complete"
  [tasks
   output-schedule
   the-task-id]
  (= (task-completion-rate tasks
                           output-schedule
                           the-task-id)
     1))

(defn work-in-progress-count
  "Work in progress is a task at the peek of the work flow [ 1 1 2 2 2 ...],
  that a resource begun treating went to the channel. Once a task is Work in progress,
  it is not involved in the reordering of tasks, unless its length 
  is equal to the original task duration : it has not yet been processed."
  [work-flow
   the-task-id]
  (count (take-while #(= the-task-id %)
                     (reverse work-flow))))

(defn task-in-work-in-progress?
  "Returns true if task is work-in-progress,
  i.e, is in the head of the work queue, and is not at full length"
  [tasks
   work-flow
   the-task-id]
  (let [wp-count (work-in-progress-count work-flow the-task-id)]
    (and (pos? 0)
         (not= (get (tasks the-task-id) :duration)
               wp-count))))

(defn all-predecessors-complete?
  "A predicate that returns true if all predecessors have been completed "
  [tasks
   output-schedule
   task-id]

  (let [the-task (get tasks task-id)
        preds (get the-task :predecessors)]

    (every? (partial task-complete?
                     tasks
                     output-schedule)
            preds)))

(defn find-fireable-tasks
 "Finds which tasks can be fired based on their predecessors."
  [tasks
   output-schedule]
  (vec
   (filter (partial all-predecessors-complete? tasks output-schedule)
           (keys tasks))))

(defn properties
  "Inspired from the joy of clojure. Knew I was going to use it someday!
  This yields a function which, applied to each task by sort-by,
  will generate vector of values used to order the tasks
  don't forget we have rows with indices, {1 {:order ...}"
  [property-names]
  (fn [row-with-index]
    (vec
     (mapcat
      #(map (comp % val) row-with-index)
      property-names ))))

(defn reorder-tasks
  "Sort tasks according to properties given in the property-names
  vector. As it is a vector, accessing from right is more effcient. as more
  urgent comes first, i.e on left of the vector, we need to reverse
  the result to put highest priority to the right."
  [tasks
   property-names]
  (vec (reverse
        (mapcat keys (sort-by (properties property-names)
                              (map (fn [[k v]] {k v}) tasks))))))

(defn tasks-for-resource
  "Given a user-id, give you all tasks for this user (with all infos)"
  [tasks resource-id]
  (filter #(= resource-id (:resource-id (val %))) tasks))

(defn task-sched-time-vector
  "Given an output-schedule, and a task-id you get a time-vector 
  of the task as present in the output schedule"
  [output-schedule
   task-id]
  (->> output-schedule
       (group-by :task-id)
       (#(get %1 task-id))
       (map :time)
       (vec)))


(defn format-a-task-in-output-schedule
  "Given a task, we compute its current time vector
  and inject begin-time and completion ratio in it"
  [output-schedule
   a-task]
  (let [ [k v] a-task
         the-tv (task-sched-time-vector output-schedule k)]

    (if (seq the-tv)
      [k (-> v
             (assoc :begin (apply min the-tv))
             (assoc :achieved (count the-tv)))]
      a-task)))


(defn format-tasks-in-output-schedule
  "Given an output schedule :
  [{:task-id 1 :time 1 :resource-id 1}
  {:task-id 3 :time 1 :resource-id 1}
  {:task-id 1 :time 2 :resource-id 1}
  {:task-id 3 :time 2 :resource-id 1}
  {:task-id 3 :time 3 :resource-id 1}]
  we find start-time, completion rate for each task and then we return
  a scheduled version of tasks. {1 {:begin 2 :completion-rate 2/5....})"
  [output-schedule
   tasks]
  (into {}
        (map (partial format-a-task-in-output-schedule
                      output-schedule)
             tasks)))

(defn work-flow-for-resource
  "given a user,  its current work-queue, tasks and current output schedule,
   we find his tasks, the fireable ones, reorder all of them (if preemptive)
   or those non work in propress if not, and issue new work-flow"
  [current-work-flow
   tasks
   resource-id
   current-output-schedule
   reordering-properties]
  (let [fireable-tasks-ids (find-fireable-tasks tasks
                                                current-output-schedule)
        fireable-tasks (select-keys tasks
                                    fireable-tasks-ids)
        his-fireable-tasks (tasks-for-resource fireable-tasks
                                               resource-id)
        his-incomplete-fireable-tasks  (into {}
                                             (filter #(not (task-complete?
                                                                tasks
                                                                current-output-schedule
                                                                (key %)))
                                                     his-fireable-tasks))
        his-incomplete-fireable-tasks-ids (keys his-incomplete-fireable-tasks)
        ;; id of the task to be kept, work in progress
        fireable-id-in-wp (first (filter (partial task-in-work-in-progress?
                                                  tasks
                                                  current-work-flow)
                                         his-incomplete-fireable-tasks-ids))
        wp-vector (vec (repeat (work-in-progress-count current-work-flow
                                                           fireable-id-in-wp)
                                   fireable-id-in-wp))
        ;; [ the part to be reordered and generated]
        fireable-ids-not-in-wp (vec
                                 (remove #(= % fireable-id-in-wp)
                                         his-incomplete-fireable-tasks-ids ))
        his-fireable-tasks-not-in-wp (select-keys tasks fireable-ids-not-in-wp)
        his-ordered-tasks-not-in-wp (reorder-tasks his-fireable-tasks-not-in-wp
                                                   reordering-properties)
        his-new-ordered-workflow (gen-work-flow tasks
                                                his-ordered-tasks-not-in-wp)]
    ;; will be used to sync the threads, on for each resource
    (into his-new-ordered-workflow wp-vector)))

(defn run-scheduler-for-resource!
  "This runs a thread for the resource-id, connects to a channel,
  and waits to process a task-unit / timer tick, until the channel is closed"
  [timer
   resource-id
   tasks
   output-schedule
   workflows
   reordering-properties
   chan-to-output]
  (let [current-flow-for-resource (@workflows resource-id)
         my-workflow (work-flow-for-resource current-flow-for-resource
                                           tasks
                                           resource-id
                                           output-schedule
                                           reordering-properties)
         the-task-unit {:task-id (peek my-workflow)
                        :time timer
                        :resource-id resource-id}
         _ (if (seq my-workflow)
             (alter workflows assoc resource-id (pop my-workflow)))]
             ;; now we inject the task-unit in the channel
    (go (>! chan-to-output the-task-unit))))

(defn total-task-duration
  "Computes total tasks duration as if they were done sequentially."
  [tasks]
  (->> tasks
       ( vals)
       (map :duration )
       (filter (comp not nil?))
       (reduce +)))

(defn run-scheduler
  "this is the master-mind. runs all of them, collects their inputs,
  and then goes home"
  [tasks
   reordering-properties]
  (let [c-to-me (chan)
        timer (ref 0)
        max-time (* 2 (total-task-duration tasks))
        workflows (ref {})
        output-schedule (ref [])
        resources-ids (set (map :resource-id (vals tasks)))]
    (dosync
      (while
          (and (< @timer max-time)
               (not (every? (partial task-complete?
                                     tasks
                                     @output-schedule) (keys tasks))))
        (alter timer inc)
        (doseq [resource resources-ids]
          ;; next tick
          ;; I fire their schedules
          (run-scheduler-for-resource! @timer
                                       resource
                                       tasks
                                       @output-schedule
                                       workflows
                                       reordering-properties
                                       c-to-me))
        (dotimes [_ (count resources-ids)]
          (alter output-schedule conj (<!! (go (<! c-to-me)))))))
    @output-schedule))


(defn missing-prop-for-task
  "Given a task ({:prop ...}) and a vector of properties
  returns missing properties for task"
  [task
    reordering-properties]
  (vec (filter (comp not nil?)
               (map #(if ((comp not (partial contains? task)) % ) %)
                    reordering-properties))))
  
(defn tasks-w-missing-properties
  "Returns a map, with task-id and a vector of missing property"
  [tasks
   reordering-properties]
    (into {} (for [[id t] tasks
                   :let [missing-props (missing-prop-for-task t
                                                              reordering-properties)]
                   :when (seq missing-props)]
               {id missing-props})))

(defn tasks-w-not-found-predecessors
  "Returns the Tasks with predecessors not declared as tasks elsewhere in the tasks definition."
  [tasks]
  (keys (filter
          (fn [[_ t]] (not (predecessors-of-task-exist? tasks
                                                        t)))
          tasks)))

(defn tasks-w-no-field
  "Which tasks don't have Field field."
  [tasks field]
   (filter (comp  not field val) tasks))

(defn tasks-w-empty-predecessors
  [tasks]
  (vec (keys (into {} (filter (comp empty? val)
                              (into {} 
                                    (map 
                                         (fn [[id t]] {id (:predecessors t)}) 
                                         tasks)))))))
(defn not-found-task?
  "If task id is not in tasks, return true"
  [tasks task-id]
  (not
   (contains?
    (set  (keys tasks))
    task-id)))

(defn tasks-w-non-existent-predecessors
  "Given Tasks, emits a detailed info on whom tasks are having non existent 
  predecessors"
  [tasks]
  (into {}
        (filter
         (comp seq #(get % 1))
         (map (fn [[id t]]
                [id (vec  (filter (partial not-found-task? tasks)
                                  (:predecessors t)))])
              tasks))))

(defn errors-on-tasks
  "verif if there-s something wrong before we schedule.
  emits a map. {:errors {} :results schedule
  errors : non existing reordering prop, non existing predecessors, and cycles."
  [tasks
   reordering-properties]
  (let [milestone-tasks (into {} (filter (comp :is-milestone val) tasks))
        non-milestone-tasks (into {} (tasks-w-no-field tasks :is-milestone))
        reordering-errors (tasks-w-missing-properties non-milestone-tasks
                                                      reordering-properties)
        tasks-graph (gen-all-precedence-edges tasks)
        tasks-cycles (graph-cycles tasks-graph)
        milestones-w-no-predecessors (tasks-w-no-field milestone-tasks
                                                       :predecessors)
        tasks-w-no-resources (tasks-w-no-field non-milestone-tasks
                                               :resource-id)]
    {:reordering-errors reordering-errors
     :tasks-w-predecessors-errors (tasks-w-non-existent-predecessors tasks)
     :tasks-w-no-resources (into [] (keys tasks-w-no-resources))
     :tasks-cycles tasks-cycles
     :milestones-w-no-predecessors  (into (into [] (keys milestones-w-no-predecessors))
                                          (tasks-w-empty-predecessors milestone-tasks))}))

(defn prepare-milestone
  "A milestone is declared by giving :milestone-name and at least one predecessor
  then we create a task, with a (gensym :milestone-user) and a duration 1 as user-id, so milestones
  can enter the scheduler algorithm"
  [a-milestone-desc]
  (assoc (assoc a-milestone-desc :duration 1)
         :resource-id (gensym :milestone-user)))

(defn prepare-tasks
  "Adds random user-ids and duration=1 to milestones tasks"
[tasks]
(let [milestone-tasks (filter (comp :is-milestone val) tasks)
      curated-milestone-tasks (map (fn [[id t]] [id (prepare-milestone t)]) milestone-tasks)]
  (into tasks curated-milestone-tasks)))

(defn schedule
  "The real over-master-uber-function to call. Gives you tasks with :begin,
  just like you'd exepct, if errors =nil, or you can read errors instead."
  [tasks
   reordering-properties]
  (let [errors (errors-on-tasks tasks
                                reordering-properties)]
    (if (every? (comp nil? seq) (vals errors))
      {:errors nil
       :result (let [curated-tasks (prepare-tasks tasks)]
                 (-> curated-tasks
                     (run-scheduler reordering-properties)
                     (format-tasks-in-output-schedule curated-tasks)))}
      {:result nil
       :errors (into {} (filter (comp not nil? val) errors))})))
