(ns milestones.yaml
  (:require #?(:clj  [yaml.core :refer [parse-string]]
               :cljs [cljsjs.js-yaml])
            [clojure.walk :refer [keywordize-keys]]
            [milestones.dyna-scheduler :as d]))

(defn parse-yaml-tasks
  "Parses a YAML string to a clj(s) datastructure."
  [yaml-str]
  (let [parsed #?(:clj (parse-string yaml-str false)
                  :cljs (->> yaml-str
                             (.load js/jsyaml yaml-str)
                             js->clj
                             (map (fn [[id task]] [(js/parseInt id) task]))))]
    (into {}
          (map keywordize-keys parsed))))
