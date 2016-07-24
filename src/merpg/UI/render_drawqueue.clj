(ns merpg.UI.render-drawqueue
  (:require [merpg.2D.core :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.layers :refer [mapwidth! mapheight!]]
            [merpg.mutable.drawing-queue :refer :all]
            [merpg.reagi :refer :all]
            [reagi.core :as r]

            [clojure.pprint :refer :all])
  (:import [java.awt Color]))

(defn update-animated-sprites! [{:keys [last-updated
                                        frame-age
                                        surface
                                        frames
                                        frame-count
                                        frame-index
                                        id
                                        playing?] :as sprite}]
  (if playing?
    (let [current-millis (System/currentTimeMillis)
          needs-updating? (> (- current-millis last-updated) frame-age)]
      (when needs-updating?
        (if-let [current-frame (get frames frame-index)]
          (let [[w h] [(img-width surface) (img-height surface)]]
            (clear! surface)
            (draw-to-surface surface
                             (Draw current-frame [0 0]))
            (re/update-registry id
                                (assoc id
                                       :frame-index (mod (inc frame-index) frame-count)
                                       :last-updated (System/currentTimeMillis))))
          (do
            (println "Frame is nil on " frame-index)
            (pprint frames))))))
  sprite)

(def load-the-map-surface (draw-to-surface (image 400 400)
                                           (Draw "Create a map in the domtree" [100 100])))

(defn render-drawqueue! [& _]
  (let [mapid (re/peek-registry :selected-map)
        [w h] [(mapwidth! mapid) (mapheight! mapid)]]
    (if (and (pos? w)
             (pos? h))
      (let [surface (image (* (mapwidth! mapid) 50)
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
                     surface)))
      load-the-map-surface)))

(def drawqueue (stream (r/sample 16 r/time)
                       (r/map render-drawqueue!)))
