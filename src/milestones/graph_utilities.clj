;;; this NS is for creating tools to manipluate graphs :
;;; generate edges from tasks, finding cycles, to see if a task depends eventually
;;; on itself

(ns milestones.graph-utilities)


(defn predecessors-of-task-exist?
  "return true if all predecessors of this task
  exist or if this task has no preds"
  [tasks the-task]
  (every?
    (partial contains? (set (keys tasks)))
    (:predecessors the-task)))

(defn task-has-predecessors?
  "return true if this task has preds"
  [the-task]
  (not (empty? (:predecessors the-task ))))


(defn gen-precendence-edge
  "a utility function, given 1 + [ 2 3] returns [1 2], [1 3]"
  [task-id predecessors]
  (mapv (fn[predecessor] [task-id predecessor]) predecessors))


(defn gen-all-precedence-edges
 "Given tasks, computes all the edges present in this graph"
  [tasks]
  (let [;; raw-maps ~ [1 [ 2 3] 2 [ 3 2]]
         raw-maps (map (fn [[k v]] [k (:predecessors v)]) tasks)]
  (mapcat (fn [[k v]] (gen-precendence-edge k v) ) raw-maps)))