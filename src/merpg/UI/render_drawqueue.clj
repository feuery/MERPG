(ns merpg.UI.render-drawqueue
  (:require [merpg.2D.core :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.layers :refer [mapwidth! mapheight!]]
            [merpg.mutable.drawing-queue :refer :all]

            [clojure.pprint :refer :all])
  (:import [java.awt Color]))

(def transparent (Color. 0 0 0 0))

(defn update-animated-sprites! [{:keys [last-updated
                                        frame-age
                                        surface
                                        frames
                                        frame-count
                                        frame-index
                                        playing?] :as sprite}]
  (if @playing?
    (let [current-millis (System/currentTimeMillis)
          needs-updating? (> (- current-millis @last-updated) frame-age)]
      (when needs-updating?
        (println "Old frame-index " @frame-index)
        (swap! frame-index #(mod (inc %) frame-count))
        (println "New frame-index " @frame-index)
        
        (let [[w h] [(img-width surface) (img-height surface)]]
          (clear! surface)
          (draw-to-surface surface
                           (Draw (get frames @frame-index)
                                 [0 0]))
          (reset! last-updated (System/currentTimeMillis))
          (println "Sprite updated")))))
  sprite)
      

(defn render-drawqueue! []
  (let [mapid (re/peek-registry :selected-map)
        surface (image (* (mapwidth! mapid) 50)
                       (* (mapheight! mapid) 50)
                       :color transparent)
        queue (get @drawing-queues-per-map mapid)]
    (->> queue
         (map (fn [{:keys [subtype] :as sprite}]
                (if (= subtype :animated)
                  (update-animated-sprites! sprite)
                  sprite)))                
         (reduce (fn [surface {x :x
                               y :y
                               angle :angle
                               name :name
                               sprite :surface}]
                   (draw-to-surface surface
                                    (Draw (if (= angle 0)
                                            sprite
                                            (rotate sprite angle))
                                            [x y])))
                 surface))))
