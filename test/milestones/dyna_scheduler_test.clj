(ns milestones.dyna-scheduler-test
  (:require [milestones.dyna-scheduler :refer :all])
  (:use expectations))

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
                 :predecessors [3]
              }})


(def correct-tasks-schedule
  (schedule correct-tasks  [:priority]))

;; some tests, we sure can do more of them

;; test if correct-tasks-schedule :errors is nil
(expect true (nil? (:error correct-tasks-schedule)))

;; test if task 6 is scheduled after 3
(expect true
        (>
        (->
          :begin
          ((:result correct-tasks-schedule) 6))
        (->
          :begin
          ((:result correct-tasks-schedule) 3))))
