;;    <GANTT Charts Drawing using Google Chart Library - Part of Automagic Tools / Milestones>
;;    Copyright (C) 2016 , Rafik NACCACHE <rafik@fekr.tech>

(ns milestones.charts)

(.load js/google.charts "current" (clj->js {:packages ["gantt"]}))

(def chart-options
  #js{:height 275

       :labelStyle {
                    :fontName "Roboto2"
                    :fontSize 14
                    :color "#757575"}

      :gantt {
              :criticalPathEnabled true
              :criticalPathStyle {:stroke "#e64a19"
                                  :strokeWidth 5}
               }
       })


(defn format-data-rows
  [tasks
   schedule-begin  ;; in RFC ISO-8601 1972-05-20T17:33:18.7Z
   time-units  ;; moment.js time units: years, months, days, hours, minutes, seconds
   ]

  (loop
      [remaining-tasks  tasks
       formatted-rows []]
    (if (seq remaining-tasks)
      (let [[task-id {:keys [task-name
                             achieved
                             begin
                             duration
                             resource-id
                             predecessors] :as task-infos}] (first remaining-tasks)]
        (recur (rest remaining-tasks)
               (conj formatted-rows [(str task-id)
                                     task-name
                                     resource-id
                                     
                                     (.add (js/moment schedule-begin)
                                           (dec begin)
                                           time-units)
                                     
                                     (.add  (js/moment schedule-begin)
                                            (dec (+ begin duration))
                                            time-units)
                                     
                                     (.asMilliseconds  (.. js/moment (duration duration "days"))) 
                                     (int (* 100 (/ achieved duration)))
                                     (if (empty? predecessors)
                                       nil
                                       (as-> predecessors p
                                         (interleave p (repeat ","))
                                         (butlast p)
                                         (reduce str "" p)))])))
      (clj->js formatted-rows))))

(defn draw-gantt!
  [tasks
   schedule-start ;; in RFC ISO-8601
   time-units ;; moment.js time units: years, months, days, hours, minutes, seconds
   in-div-id
   options]
  (let [data  (new js/google.visualization.DataTable)
        data-rows (format-data-rows tasks schedule-start time-units)]

    ;; Defining the columns of the GANTT
    (doto data
      (.addColumn  "string" "Task ID")
      (.addColumn  "string" "Task Name")
      (.addColumn  "string" "Resource")
      (.addColumn  "date" "Start Date")
      (.addColumn  "date" "End Date")
      (.addColumn  "number" "Duration")
      (.addColumn  "number" "Percent Complete")
      (.addColumn  "string" "Dependencies"))

    ;; Adding DataRows
    (.addRows data data-rows)
    ;; Drawing
    (.draw (new js/google.visualization.Gantt
                (.getElementById js/document in-div-id))
           data
           (clj->js options))))









