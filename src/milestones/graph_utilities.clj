;;; this NS is for creating tools to manipluate graphs :
;;; generate edges from tasks, finding cycles, to see if a task depends eventually
;;; on itself

(ns milestones.graph-utilities
  (:require
    [loom.graph :refer [digraph nodes]]
    [loom.alg :refer [bf-path]]
    [loom.io :refer [view]]
    [clojure.math.combinatorics :refer [combinations]]))


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

(defn graph-cycles
  "tests for every couple a and b if there is a path
  btw a -> b and b-> : this is a cycle. edges is a digraph (loom)"
  [edges]

  (let [di-graph (apply digraph edges)
        vertices (vec(nodes di-graph) )
        all-paths (combinations vertices 2)]

    (vec (filter (fn [[l r]] (and  (seq (bf-path di-graph l r))
                              (seq (bf-path di-graph r l))) )
            all-paths))))