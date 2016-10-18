(ns milestones.nlp-tools
  (:require [milestones.parser-rules :refer [rules item-significant-value?]]))


(def nlp (.-nlp_compromise js/window))

(def lexicon (.lexicon nlp))
(aset lexicon "task" "Task")
(aset lexicon "tasks" "Task")
(aset lexicon "milestone" "Milestone")
(aset lexicon "milestones" "Milestone")
(aset lexicon "with priority" "Priority")
(aset lexicon "in order" "InOrder")
(aset lexicon "when" "Predecessors")
(aset lexicon "after" "Predecessors") ;; to avoid amboguity in the optional steps

(defn pos-tags-lexicon
  [lexicon
   sentence]
  (let [nlp-sentence (.sentence nlp sentence #js {:lexicon lexicon})
        nlp-terms (.-terms nlp-sentence)]
    (map
     (fn [term] [(js->clj  (.-text term))
                 (js->clj   (.-pos term) :keywordize-keys true )])
     nlp-terms)))

(def pos-tags (partial pos-tags-lexicon lexicon))

;; A stack for a recursive descent parser, containing what keys to accept at every  stage:
;; '( ...       #{:noun :verb}  ...        :a-step)
;;  at this stage --^  must be found -----------^ or this is a step to do something


(defn matches?
  [input-item
   current-tag-alternatives]
  (some #{(as-> input-item i
                (get i 1)
                (set (keys i)))} current-tag-alternatives))

(defn accept-tag
  "Verifies if an input like: [\"task\" {:Noun true}] correponds to
  one of the keys stored in the head of tag-stack, which would be an
  element like #{ :multi #{:Noun Value...}}, or if it is a checkpoint,
  to notify the caller to construct a part of the task. the :star in a
  head of stack means that this token can be met multiple times,
  causing the stack to keep it when ever we find an item corresponding
  to it, or consuming it an moving forward if the element correspond
  to the next status."
  [input-item
   tag-stack]
  ;; pick the tag that should be there in the stack

  (if-let [current-tag-alternatives (first tag-stack)] ;; #{[:noun :verb ]...}
    (cond
      (keyword? current-tag-alternatives) {:step current-tag-alternatives
                                           :new-stack (rest tag-stack)} 

      
      ;; It corrsponds to one of the alternatives
      (matches? input-item current-tag-alternatives) (if (contains? current-tag-alternatives :multi)
                                                       {:step false
                                                        :new-stack  tag-stack}
                                                       {:step false
                                                        :new-stack (rest tag-stack)})
      ;; If I'm here, input-tem does-not match the head of the stack. If the head contains
      ;; Soemthing :multi, let's see if its following status correspond to our item so we can move forward
      
      (contains? current-tag-alternatives :multi) (if (matches? input-item (second tag-stack))
                                                    {:step false
                                                     :new-stack (-> tag-stack rest rest)}
                                                    ;; nope, even the thing after the :multi stage doesn't match.
                                                    ;; that won't do.
                                                    false)
      
      :default false)
    false))

(defn fast-forward
  "Goes FFW in a tag-stack until it finds a step specification. "
  [tag-stack]
  (if (seq tag-stack)
    (if (keyword? (first tag-stack))
      tag-stack
      (recur (rest tag-stack)))
    nil))

(defn parse-task-w-a-tag-stack
  [task-str
   init-tag-stack
   optional-steps]
  (loop
      [input-items (->> task-str
                        pos-tags
                        (filter (comp not empty? #(get % 1))))
       tag-stack init-tag-stack
       output-stack {}
       output {}]
    (if (seq input-items)
      (let [input-item (first input-items)]
        (if-let [{:keys [step new-stack]}
                 (accept-tag input-item tag-stack)]
          ;; parse if successful, let's see:
          (cond
            step (recur input-items
                        new-stack
                        {:step step :items []}
                        (if  (empty? (get output-stack :items))
                          output
                           (assoc output (get output-stack :step)
                                 (get  output-stack :items))))
            :default (recur (rest input-items)
                            new-stack
                            (if (item-significant-value? input-item (get output-stack :step))
                              (merge-with conj output-stack {:items (get  input-item 0)})
                              output-stack) 
                            output))
          
          (if (some #{(get output-stack :step)} optional-steps)
            
              (if-let [ffw-stack (fast-forward tag-stack)]
                (recur input-items
                       ffw-stack
                       output-stack
                       output)
                {:error {:step (get output-stack  :step) :expected (first tag-stack) :item input-item}}
                )
            {:error {:step (get output-stack  :step) :expected (first tag-stack) :item input-item}})))
      ;; Items are finished. Stack must be either empty, or contains a :multi kind of item
      (if  (or (empty? tag-stack)
               (contains? (first tag-stack) :multi))
        
        {:error false  :result  (assoc output (get output-stack :step)
                                       (get  output-stack :items))}
        {:error {:step (get output-stack :step) :expected (first tag-stack) :item nil }}))))



(defn parse-tags-rules
  "Tries to parse the sentence according to rules (tag stacks). If it finds a
  match, will return it. else, it'll return the errors it found"
  [rules
   sentence
   optional-steps]
  (loop [rem-rules rules
         errors []]
    (if (seq rem-rules)
      (let  [
             cur-rule (first rem-rules)
             cur-parse-result (parse-task-w-a-tag-stack sentence cur-rule optional-steps)]
        (if-let [err (get cur-parse-result :error)]
          (recur (rest rem-rules)
                 (conj errors err))
          {:errors nil
           :result (get cur-parse-result :result)}))
      {:errors errors})))


(def parse-tags
  (partial parse-tags-rules rules))

(defn curate-task  
  "Curates generated tasks : 1,2,3... => [1 2 3]
  [d] -> d, [5] -> 5"
  [a-task]
  (let [{:keys [predecessors]} a-task
        _ (println predecessors)]
    {
     :predecessors (->>
                    (get predecessors 0)
                    (.text nlp)
                    .terms   
                   (mapv #(.root %)))
                   }
    )
  )
