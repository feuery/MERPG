(ns merpg.core
  (:require [merpg.UI.main-layout :refer [show-mapeditor]]
            [seesaw.core :refer [native!]]
            [clojure.pprint :refer :all]
            [merpg.mutable.tileset]
            [merpg.mutable.maps]
            [merpg.mutable.registry :as re]
            [merpg.settings.core :as settings]
            [merpg.settings.nrepl]
            [merpg.IO.out :refer [read-image!]]
            [merpg.game.core :refer [run-game!]]
            [merpg.reagi :refer :all])
  (:gen-class))

(settings/fire-events!)

(defn -main [& args]  
  (native!)
  (reset! re/render-allowed? true)
  (if (nil? args)
    (do
      (swap! re/registry identity)
      (show-mapeditor))
    (let [[cmd path] args]
      (when (= cmd "--image")
        (println "Reading " path)
        (if (read-image! path)
          (println "Read " path)
          (println "Failure while reading " path)))
      (run-game!))))
    

(defn main []
  (-main))

;; (main)
