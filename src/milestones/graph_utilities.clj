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


(defn connected-edges?
  "return true if we have something like this :
  [a b ] [b c] or [b c] and [a b]"
  [[l1 r1] [l2 r2]]
  (or (= r2 l1 )
      (= r1 l2)))


(defn can-be-connected-to-stack?
  " returns true if this edge can be connected to this stack"
  [stack-of-edges edge]
  (some (partial connected-edges? edge) stack-of-edges))


(defn put-edge-in-stacks!
  "given a stacks map {1 [[1 2] [3 4]...}, returns the new-stacks that edge [1 2]can be connected with
  else, it generates a nex idx and alters the stacks-map
  I think it's inspired from tarjan's Algo, though I did just read the def *VERY* quickly"
  [stacks-map-ref  latest-idx-ref edge]
  (let [the-stacks (filter #(can-be-connected-to-stack? (val %) edge)
                           @stacks-map-ref)]
    (if (empty? the-stacks)
      ;; I create a new IDX, and put this edge in it
      (alter stacks-map-ref assoc (alter latest-idx-ref inc) [edge] )
      ;; else, for every idx found, I add this
      (doseq [idx (keys the-stacks)]
        (alter stacks-map-ref (partial merge-with conj) {idx edge} )))))

(defn cluster-connected-edges
  [edges]
  (let [latest-idx (ref 0)
        stacks-map (ref {})]
    (dosync
      (doseq [edge edges]
        (put-edge-in-stacks! stacks-map latest-idx edge)))
    @stacks-map))

(defn merge-two-neighbors
 "given two edges, genrates the *vectorial* sum of the two, by finding the common
 vertex"
  [[l1 r1] [l2 r2]]
  (cond
    (= r1 l2) [l1 r2]
    (= r2 l1) [l2 r1]
    :else nil))

(defn find-a-next
  "finds a next neighbor to be used to construct paths"
  [[_ r] edges]
  (first (filter #(= r (get % 0)) edges)))

(defn longest-path
  "Where can I go picking first elt and jumping next to next?
  if it is a cycle, I must get my first element."
  [edges]
  (loop [current  (first edges)
         remaining (rest edges)
         next-edge (find-a-next current remaining)
         result current]


    (if (or  (not next-edge)
             (not (seq remaining)))
      ;; I return what I had so far, I don't have no more next edges
      ;; for the edge I've chosen, or I don't have no more edges (no cycle
      ;; but longest path -and it's connected

      result
      ;; else recur using the next to current, the remaining minus the next, and
      ;; result merged with merge neighbors
      (let [next-current next-edge
            next-remaining (vec (remove (partial = next-edge ) remaining))
            next-next-edge (find-a-next next-edge next-remaining)
            next-result (merge-two-neighbors result next-edge)]
        (recur next-current
               next-remaining
               next-next-edge
               next-result)))))

(defn graph-has-cycles
  "tells if your edges can have cycles.
  returns {:cycles [vector of tasks involved in cycles or nil if all is ok"
  [edges]
  (let [connected-components (vals (cluster-connected-edges edges ))
        longest-paths (mapv longest-path connected-components)
        cycles  (filter (fn[[l r]] (= l r ) ) longest-paths )]
    (if cycles {:cycles (mapv first cycles)}
               {:cycles nil})))

