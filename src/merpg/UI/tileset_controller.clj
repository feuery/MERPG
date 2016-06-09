(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.immutable.basic-map-stuff :refer [tile]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.pprint :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn tileset-controller []
  (:canvas (bindable-canvas (atom nil)
                   (fn [_]
                     (draw-to-surface (image 100 100 :color "#FFFFFF")
                                      (Draw "TODO tileset-controller is broken" [0 0]))))))
