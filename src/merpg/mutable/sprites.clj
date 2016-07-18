(ns merpg.mutable.sprites
  (:require [merpg.mutable.registry :as re]
            [merpg.UI.events :as e]
            [merpg.2D.core :refer :all]

            [reagi.core :as r]
            [clojure.pprint :refer :all]
            ;; [seesaw.core :refer :all]
            )
  (:import [java.awt Color]
           [java.awt.image BufferedImage]))

(defn static-sprite! [map-id path]
  (let [sprites-per-map (count (re/query! #(and (= (:type %) :sprite)
                                                (= (:parent-id %) map-id))))]
    (e/allow-events
     (re/register-element! {:name "New sprite"
                            :type :sprite
                            :subtype :static
                            :order sprites-per-map
                            :parent-id map-id
                            :x 0
                            :y 0
                            :angle 0.0
                            :surface (image path)}))))

(defn animated-sprite! [map-id path frame-amount]
  (let [sprites-per-map (count (re/query! #(and (= (:type %) :sprite)
                                                (= (:parent-id %) map-id))))
        spritesheet (image path)
        frame-h (img-height spritesheet)
        frame-w (/ (img-width spritesheet)
                   frame-amount)
        id (keyword (gensym "ANIM__"))]
    (e/allow-events
     (re/register-element! id {:name "New animation"
                               :id id
                               :type :sprite
                               :subtype :animated
                               :order sprites-per-map
                               :parent-id map-id
                               :x 0
                               :y 0
                               :angle 0.0
                               :surface (image frame-w frame-h :color (Color. 0 0 0 0))
                               :frames (->> (range 0 (img-width spritesheet) frame-w)
                                            (mapv (fn [frame-x]
                                                    (draw-to-surface (BufferedImage. frame-w frame-h BufferedImage/TYPE_INT_ARGB)
                                                                     (Draw (subimage spritesheet frame-x 0 frame-w frame-h) [0 0])))))
                               
                               :playing? true
                               :last-updated (System/currentTimeMillis)
                               :frame-age 38 ;; millis
                               :frame-index 0
                               :frame-count frame-amount}))))

;; (defn show-img [img]
;;   (frame :content
;;          (canvas :paint #(.drawImage %2 img 0 0 nil))
;;          :width (* 2 (img-width img))
;;          :height (* (img-height img) 2)
;;          :visible? true))

(defn animation->spritesheet [{:keys [subtype
                                      frames]}]
  {:pre [(= subtype :animated)
         (some? frames)
         (pos? (count frames))]}
  (let [w (->> frames
               (map img-width)
               (reduce +))
        step (/ w (count frames))
        h (img-height (first frames))
        xs (vec (range 0 w step))]
    (assert (= (count xs) (count frames)))
    (draw-to-surface (clear! (image w h))
                     (dotimes [i (count frames)]
                       (if-let [frame (get frames i)]
                         (Draw frame
                               [(get xs i) 0])
                         (do
                           (println "Frame is nil at " i)))))))
  
