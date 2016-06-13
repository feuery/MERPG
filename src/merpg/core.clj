(ns merpg.core
  (:require [merpg.immutable.basic-map-stuff :refer [make-map]]
            [merpg.UI.main-layout :refer [show-mapeditor]]
            [seesaw.core :refer [native!]]
            [merpg.mutable.tileset]
            [merpg.mutable.maps])
  (:gen-class))

(defn -main [& args]
  (native!)
  (show-mapeditor))

(defn main []
  (-main))

;; (main)
