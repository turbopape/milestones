(ns milestones.dyna-scheduler-test
  (:require [milestones.dyna-scheduler :refer :all])
  (:use expectations))

(def tasks {1 {:task-name "Bring bread"
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

;; TODO - write tests with begin and errors etc...