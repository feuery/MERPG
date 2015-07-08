(ns merpg.core
  (:require [merpg.immutable.basic-map-stuff :refer [make-map]]
            [merpg.UI.main-layout :refer [show-mapeditor]])
  (:gen-class))

(defn -main [& args]
  (def root {:maps (atom [(make-map 10 10 1)])})
  (show-mapeditor (:maps root)))
