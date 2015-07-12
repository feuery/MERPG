(defproject memapper "0.1.0"
  :description "A map editor for the MERPG-game. Might morph into a real 2d-game-building-environment some day"
  :url "http://yearofourlord.blogspot.com"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [seesaw "1.4.4"]
                 [environ "0.5.0"]]
  :plugins [[cider/cider-nrepl "0.9.1"]]
  :main merpg.core
  :profiles {:uberjar {:aot :all}})
