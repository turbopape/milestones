(ns milestones.nlp-tools)

(def nlp (.-nlp_compromise js/window))

(defn pos-tags
  [sentence]
  (let [nlp-sentence (.sentence nlp sentence)
        nlp-terms (.-terms nlp-sentence)]
    (mapv
     (fn [term] [(js->clj  (.-text term))
                 (js->clj   (.-pos term) :keywordize-keys true )])
     nlp-terms)))



