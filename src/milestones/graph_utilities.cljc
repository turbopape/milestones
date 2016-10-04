;;    <Graph Utilities - Part of Automagic Tools / Milestones>
;;    Copyright (C) 2016 , Rafik NACCACHE <rafik@fekr.tech>

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

(defn vertices
  [edges]
  (->> edges
       (mapcat identity )
       set))

(defn successors
  [vertex edges]
  (->> edges 
       (filter (comp (partial = vertex) first))
       (map second)))

(defn graph-cycles
  "Uses [Tarjan]((https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm)'s
  strongly connectect components algorithm to find if there are any
  cycles in a graph"
  [edges]
  (let [index (atom 0)
        indices (atom {}) ;;{vertex index, ...}
        lowlinks (atom {})
        S (atom (list));;{vertex lowlink}
        output (atom [])]
    (letfn [(strong-connect [v]
             (swap! indices assoc-in  [v] @index)
             (swap! lowlinks assoc-in [v] @index)
             (swap! index inc)
             (swap! S conj v)
             (let [succs (successors v edges)]
               (doseq [w succs]
                 (if (not (contains? @indices w))
                   (do (strong-connect w)
                       (swap! lowlinks assoc-in [v] (min (get @lowlinks v)
                                                         (get @lowlinks w))))
                   (if (some #{w} @S )
                     (swap! lowlinks assoc-in [v] (min (get @lowlinks v)
                                                       (get @indices w))))))
               (if (= (get @lowlinks v)
                      (get @indices v))
                 (loop [w (peek @S)
                        r []]
                   (swap! S pop)
                   (if (not (= v w))
                     (recur (peek @S)
                            (conj r  w))
                     (when-not (empty? r)
                         (swap! output conj (conj  r w))))))))]
      (doseq [v (vertices edges)]
        (when-not (get @indices v)
          (strong-connect v)))
      @output)))
