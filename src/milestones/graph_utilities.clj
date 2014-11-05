;;    <Graph Utilities - Part of Automagic Tools / Milestones>
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


(ns milestones.graph-utilities
  (:require
    [loom.graph :refer [digraph nodes]]
    [loom.alg :refer [bf-path]]
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
  (seq (:predecessors the-task)))

(defn gen-precendence-edge
  "a utility function, given 1 + [ 2 3] returns [1 2], [1 3]"
  [task-id predecessors]
  (mapv (fn[predecessor] [task-id predecessor]) predecessors))

(defn gen-all-precedence-edges
 "Given tasks, computes all the edges present in this graph"
  [tasks]
  (let [raw-maps (map (fn [[k v]] [k (:predecessors v)])
                      tasks)]
    (mapcat (fn [[k v]] (gen-precendence-edge k v) ) raw-maps)))

(defn graph-cycles
  "tests for every couple a and b if there is a path
  btw a -> b and b-> : this is a cycle. edges is a digraph (loom)"
  [edges]
  (let [di-graph (apply digraph edges)
        vertices (vec(nodes di-graph) )
        all-paths (combinations vertices 2)]
    (mapv vec (filter (fn [[l r]] (and  (seq (bf-path di-graph l r))
                              (seq (bf-path di-graph r l))) )
                      all-paths))))
