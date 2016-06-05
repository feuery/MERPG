(defproject memapper "0.1.1-snapshot"
  :description "A map editor for the MERPG-game. Might morph into a real 2d-game-building-environment some day"
  :url "http://yearofourlord.blogspot.com"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [seesaw "1.4.4"]
                 [environ "0.5.0"]
                 [prismatic/schema "1.1.1"]
                 ;; [org.clojure/tools.nrepl "0.2.12"]
                 ]
  ;; :plugins [[cider/cider-nrepl "0.10.2"]]
  :main merpg.core
  :java-source-paths ["java-src"]
  :profiles {:uberjar {:aot :all}}
  )
