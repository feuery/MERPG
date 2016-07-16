(ns merpg.UI.render-drawqueue
  (:require [merpg.2D.core :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.layers :refer [mapwidth! mapheight!]]
            [merpg.mutable.drawing-queue :refer :all]

            [clojure.pprint :refer :all])
  (:import [java.awt Color]))

(defn render-drawqueue! []
  (let [mapid (re/peek-registry :selected-map)
        surface (image (* (mapwidth! mapid) 50)
                       (* (mapheight! mapid) 50)
                       :color (Color. 0 0 0 0))
        queue (get @drawing-queues-per-map mapid)]
    (->> queue
         (reduce (fn [surface {x :x
                               y :y
                               name :name
                               sprite :surface}]
                   (println "Rendering " name " at " [x y])
                   (draw-to-surface surface
                                    (Draw sprite [x y])))
                 surface))))
