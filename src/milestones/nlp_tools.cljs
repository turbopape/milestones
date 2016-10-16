(ns milestones.nlp-tools)

(def nlp (.-nlp_compromise js/window))

(def lexicon (.lexicon nlp))
(aset lexicon "task" "Task")
(aset lexicon "milestone" "Milestone")

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


(def tasks-tag-stack
  '(:task-id
    #{:Task}
    #{:Noun :Value}
    :resource-id
    #{:Noun}
    :task-name
    #{:Verb :Modal}
    #{:Verb :Infinitive}
    #{:Noun}
    :duration
    #{:Preposition }
    #{:Noun :Plural :Date}
    :predecessors
    #{:Preposition :Condition}
    #{:Task :Condition}
    #{:Noun :Value :Condition}))

(defn item-significant-value?
  [input-item]
  (cond
    (get (get input-item 1) :Preposition ) false
    (get (get input-item 1) :Task) false
      :default true))

(defn accept-tag
  "Verifies if an input like: [\"task\" {:Noun true}] correponds to
  one of the keys stored in the head of tag-stack, or if it is a
  checkpoint, to notify the caller to construct a part of the task."
  [input-item
   tag-stack]
  ;; pick the tag that should be there in the stack

  (if-let [current-tag-alternatives (first tag-stack)] ;; #{[:noun :verb ]...}
    (cond
      (keyword? current-tag-alternatives) {:step current-tag-alternatives
                                           :new-stack (rest tag-stack)} 
      ;; It corrsponds to one of the alternatives
      (= (as-> input-item i
           (get i 1)
           (set (keys i))) current-tag-alternatives) {:step false
                                                      :new-stack (rest tag-stack)}
      :default false)
    false))

(defn parse-task-w-a-tag-stack
  [task-str
   init-tag-stack]
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
                            (if (item-significant-value? input-item)
                              (merge-with conj output-stack {:items (get  input-item 0)})
                              output-stack
                              ) 
                            output)) 
          {:error true}))
      {:error false  :result  (assoc output (get output-stack :step)
                                 (get  output-stack :items))   })))



(defn curate-items
  [item-vector]
  
  )
