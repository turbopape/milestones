(defproject automagic-tools-milestones "0.1.4"
  :description "Milestones : the Automagic Project Tasks Scheduler"
  :url "http://automagic.tools/milestones"
  :license {:name "GNU GPL v3.0"
            :url "https://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/math.combinatorics "0.0.8"]
                 [aysylu/loom "0.5.0"]
                 [expectations "2.0.9"]]
  :scm {:name "git"
        :url "https://github.com/automagictools/milestones.git"}
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-expectations "0.0.8"]]}})
