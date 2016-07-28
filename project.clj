(defproject memapper "0.1.2-SNAPSHOT"
  :description "A map editor for the MERPG-game. Might morph into a real 2d-game-building-environment some day"
  :url "http://yearofourlord.blogspot.com"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [seesaw "1.4.5"]
                 [environ "0.5.0"]
                 [prismatic/schema "1.1.1"]
                 [reagi "0.10.1"]
                 [cider/cider-nrepl "0.12.0"]
                 [org.clojure/tools.nrepl "0.2.12"]]
  :main merpg.core
  :java-source-paths ["java-src"]
  :jvm-opts ["-Dsun.java2d.opengl=True"
             "-Dsun.java2d.accthreshold=0"]
  :profiles {:uberjar {:aot :all}})
