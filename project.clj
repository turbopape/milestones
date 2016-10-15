(defproject org.clojars.turbopape/milestones "0.2.1"
  :description "Milestones : the Automagic Project Tasks Scheduler"
  :url "http://turbopape.github.io/milestones"
  :license {:name "MIT" 
            :url "http://opensource.or g/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [expectations "2.1.8"]]

  :clean-targets ^{:protect false} ["target"]
 
  :plugins [[lein-cljsbuild "1.1.4"]]

  :cljsbuild {
              :builds [{:id "milestones"
                        :source-paths ["src"]
                        
                        :compiler {:output-to "target/out/milestones.js"
                                   :output-dir "target/out"
                                   :optimizations :none
                                   :source-map true}}]}
  
  :scm {:name "git"
        :url "https://github.com/turbopape/milestones.git"}

  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-expectations "0.0.8"]]}})
