(ns milestones.dyna-scheduler-test
  (:require [milestones.dyna-scheduler :refer :all])
  (:use expectations))

(def tasks {1 {:task-name "A description about this task"
               :resource-id 2
               :duration 5
               :priority 1
               :predecessors []}

             2 {:task-name "A description about this task"
              :resource-id "rafik"
              :duration 5
              :priority 1
              :predecessors []}

             3 {:task-name "A description about this task"
              :resource-id 4
              :duration 3
              :priority 1
              :predecessors []}

             4 {:task-name "A description about this task"
                :resource-id 4
                :priority 1
                :predecessors [2 4]}

             5 {:task-name "A description about this task"
                :resource-id "rafik"
                :duration 10
                :priority 4
                :predecessors [3]}})