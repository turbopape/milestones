(ns milestones.dyna-scheduler-test
  (:require [milestones.dyna-scheduler :refer :all]
            [clojure.test :refer :all]))

(def correct-tasks {1 {:task-name "Bring bread"
                       :resource-id "mehdi"
                       :duration 5
                       :priority 1
                       :predecessors []}

                    2 {:task-name "Bring butter"
                       :resource-id "rafik"
                       :duration 5
                       :priority 1
                       :predecessors []}

                    3 {:task-name "Put butter on bread"
                       :resource-id "salma"
                       :duration 3
                       :priority 1
                       :predecessors [1 2]}

                    4 {:task-name "Eat toast"
                       :resource-id "rafik"
                       :duration 4
                       :priority 1
                       :predecessors [3]}

                    5 {:task-name "Eat toast"
                       :resource-id "salma"
                       :duration 4
                       :priority 1
                       :predecessors [3]}

                    ;; now some milestones
                    6 {:task-name "Toasts ready"
                       :is-milestone true
                       :predecessors [3]}})

(def correct-tasks-schedule
  (schedule correct-tasks  [:priority]))

;; test if correct-tasks-schedule :errors is nil
(deftest correct-tasks-pass
  (testing "Correct tasks have no error.")
  (is (= true
         (nil? (:error correct-tasks-schedule)))))

;; test if task 6 is scheduled after 3
(deftest tasks-are-correctly-scheduled)
(testing "test if task 6 is scheduled after 3")
(is (= true
       (>
        (get-in correct-tasks-schedule [:result 6 :begin] )
        (get-in correct-tasks-schedule [:result 3 :begin] ))))

;; Detecting cycles

(def cyclic-tasks {1 {:task-name "task 1"
                      :resource-id "mehdi"
                      :duration 5
                      :priority 1
                      :predecessors [3]}

                   2 {:task-name "task 2"
                      :resource-id "rafik"
                      :duration 5
                      :priority 1
                      :predecessors [1]}

                   3 {:task-name "task 3"
                      :resource-id "salma"
                      :duration 3
                      :priority 1
                      :predecessors [2]}})

(deftest has-cycle-errors
  (testing "Detect cycles in tasks definition graphs.")
  (is  (= [[2 3 1]]
          (get-in
           (schedule cyclic-tasks  [:priority])
           [:errors :tasks-cycles]))))

;; task 3 has no resource but is no milestone,
;; task 1 is a milestone, so only task 3 should be reported
(def tasks-w-no-resource {3 {:task-name "task 3"
                             :duration 3
                             :priority 1
                             :predecessors []}

                          1 {:task-name "milestone 1"
                             :duration 3
                             :priority 1
                             :is-milestone true
                             :predecessors [3]}})

(deftest verify-resources-for-tasks
  (testing " task 3 has no resource but is no milestone,task 1 is a milestone, so only task 3 should be reported")
  (is  (=
        [3]
        (get-in
         (schedule tasks-w-no-resource [:priority])
         [:errors :tasks-w-no-resources]))))


;; some tasks with inexisting predecessors
(def tasks-w-predecessors-errors {3 {:task-name "task 3"
                                     :duration 3
                                     :priority 1
                                     :predecessors [17]}

                                  1 {:task-name "task 1"
                                     :duration 3
                                     :priority 1
                                     :predecessors []}})
(deftest tasks-inexistent-preds
  (testing "Some tasks with inexisting predecessors")
  (is 
   (=
    {3 [17]}
    (get-in
     (schedule tasks-w-predecessors-errors [:priority])
     [:errors :tasks-w-predecessors-errors]))))

;; Some tasks that do not contain the ordering field
(def tasks-w-reordering-errors {3 {:task-name "task 3"
                                   :duration 3
                                   :predecessors []}
                                
                                1 {:task-name "task 1"
                                   :duration 3
                                   :priority 1
                                   :predecessors [3]}})

(deftest tatks-reordering-issues
  (testing "Some tasks that do not contain the ordering field")
  (is  (=
        {3 [:priority]}
        (get-in
         (schedule tasks-w-reordering-errors [:priority])
         [:errors :reordering-errors]))))


;; Milestones with no predecessors errors
(def milestones-w-no-predecessors  {3 {:task-name "task 3"
                                       :duration 3
                                       :priority 1
                                       :predecessors []}

                                    1 {:task-name "milestone 1"
                                       :duration 3
                                       :priority 1
                                       :is-milestone true
                                       :predecessors []}})
(deftest tasks-w-no-preds
  (testing "Some tasks that do not contain the ordering field")
  (is  (=
        [1]
        (get-in
         (schedule milestones-w-no-predecessors [:priority])
         [:errors :milestones-w-no-predecessors]))))
