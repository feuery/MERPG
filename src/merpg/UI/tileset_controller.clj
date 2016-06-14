(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.immutable.basic-map-stuff :refer [tile]]
            [merpg.events.mouse :refer [post-mouse-event!]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.pprint :refer :all]

            [merpg.mutable.tileset-rview :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn tileset-controller []
  (let [mouse-visited-map (atom nil)
        c (canvas :paint
                  (fn [_ g]
                    (if (realized? rendered-tilesets)
                      (.drawImage g (get @rendered-tilesets @selected-tileset-ui) nil 0 0)
                      (.drawImage g (image 100 100 :color "#00FF00") nil 0 0)))
                  :listen
                  [:mouse-pressed
                   (fn [e]
                     (when (realized? rendered-tilesets)
                       (let [surface (get @rendered-tilesets @selected-tileset-ui)
                             [w h] (mapv #(/ % 50) [(img-width surface)
                                                    (img-height surface)])]
                         (reset! mouse-visited-map (->> false
                                                        (repeat h)
                                                        vec
                                                        (repeat w)
                                                        vec)))))
                   :mouse-dragged (fn [e]
                                    (let [[x-pxl y-pxl] (mouse-location e)
                                          [x-tile y-tile] [(long (/ x-pxl 50))
                                                           (long (/ y-pxl 50))]]
                                      (when-not (get-in @mouse-visited-map [x-tile y-tile])
                                        (post-mouse-event! x-pxl y-pxl :tileset-controller)
                                        (swap! mouse-visited-map update-in [x-tile y-tile] not))))])]
    (remove-rtileset-watcher :tileset)
    (add-rtileset-watcher #(repaint! c) :tileset)
    (scrollable c)))
