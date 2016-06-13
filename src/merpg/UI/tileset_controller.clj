(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.immutable.basic-map-stuff :refer [tile]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.pprint :refer :all]

            [merpg.mutable.tileset-rview :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn tileset-controller []
  (let [c (canvas :paint
                  (fn [_ g]
                    (if (realized? rendered-tilesets)
                      (.drawImage g (get @rendered-tilesets @selected-tileset-ui) nil 0 0)
                      (.drawImage g (image 100 100 :color "#00FF00") nil 0 0))))]
    (remove-rtileset-watcher :tileset)
    (add-rtileset-watcher #(repaint! c) :tileset)
    (scrollable c)))
