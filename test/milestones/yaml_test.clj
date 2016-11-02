(ns milestones.yaml-test
  (:require [milestones.yaml :as y]
            [milestones.dyna-scheduler :refer [schedule]]
            [expectations :refer [expect]]))

(def yaml-tasks
"1:
   task-name : Bring bread
   resource-id : mehdi
   duration : 5
   priority : 1
   predecessors : []
2:
  task-name : Enjoy bread
  resource-id : rafik
  duration : 3
  priority : 1
  predecessors :
     - 1")

(expect {1 {:task-name "Bring bread"
            :resource-id "mehdi"
            :duration 5
            :priority 1
            :predecessors []}

         2 {:task-name "Enjoy bread"
            :resource-id "rafik"
            :duration 3
            :priority 1
            :predecessors [1]}}
        (y/parse-yaml-tasks yaml-tasks))

;; Check scheduling accepts the yaml parsed format
(def scheduled (schedule (y/parse-yaml-tasks yaml-tasks) [:priority]))
(expect true (nil? (:error scheduled)))
