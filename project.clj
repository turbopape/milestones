(defproject automagic-tools-milestones "0.1.0"
  :description "Milestones : the automagic project Scheduler"
  :url "http://automagic.tools/milestones"
  :license {:name "GNU GPL v3.0"
            :url "https://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [expectations "2.0.9"]]

  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[com.cemerick/austin "0.1.5"]
                             [lein-expectations "0.0.8"]]}})
