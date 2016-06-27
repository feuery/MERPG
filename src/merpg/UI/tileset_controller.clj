(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.draggable-canvas :refer :all]
            [merpg.events.mouse :refer [post-mouse-event!]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.pprint :refer :all]
            
            [merpg.mutable.tileset-rview :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn get-tileset-surface! []
  (if (realized? rendered-tilesets)
    (get @rendered-tilesets @selected-tileset-ui)
    (image 100 100 :color "#00FF00")))

(defn tileset-controller []
  (let [c (draggable-canvas :paint-fn
                            (fn [_ g]
                              (.drawImage g (get-tileset-surface!) nil 0 0))
                            :draggable-fn
                            (fn [e]
                              (let [[x-pxl y-pxl] (mouse-location e)]
                                (post-mouse-event! x-pxl y-pxl :tileset-controller)))
                            :surface-provider get-tileset-surface!)]
    (remove-rtileset-watcher :tileset)
    (add-rtileset-watcher #(repaint! c) :tileset)
    (scrollable c)))
