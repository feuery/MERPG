(ns merpg.pxart.video-player
  (:require [seesaw.core :refer :all :exclude [width height]]
            [seesaw.bind :as b]
            [merpg.UI.map-controller :refer [show]]
            [merpg.2D.core :refer :all]
            [merpg.immutable.basic-map-stuff :refer [layer-name width height]]
            [merpg.UI.BindableList :refer [bindable-list]]
            [merpg.pxart.colors :refer :all]))


(def f (frame :width 600
              :height 400
              :title "Animation player"
              :visible? true))

(defn get-content [frameset]
  {:pre [(not (nil? frameset))]}
  (def running? (atom false))
  (def time-per-frame (atom 1000));; Milliseconds
  (def frame-index-atom (atom 0))
  
  (let [frame-list (bindable-list (atom frameset) frame-index-atom :custom-model-bind layer-name) ;; Bindable-list takes care of the index-atom...
        buffer (image (* 10 (width (first frameset)))
                      (* 10 (height (first frameset))))
        
        timer-container (atom [])
        canv (canvas :paint (fn [_ g]
                              (.drawImage g
                                          (draw-to-surface buffer
                                                           (with-color "#FFFFFF"
                                                             (Rect 0 0 (img-width buffer) (img-height buffer) :fill? true))
                                                           (dotimes [x (width (first frameset))]
                                                             (dotimes [y (height (first frameset))]
                                                               (with-color (number->color (get-in frameset [@frame-index-atom x y]))
                                                                 (Rect (* 10 x) (* 10 y) 10 10 :fill? true)))))
                                          0 0 nil)))]
    (left-right-split canv
                      (vertical-panel
                       :items [
                               (button :text "Play"
                                       :listen
                                       [:action
                                        (fn [_]
                                          (when-not @running?
                                            (swap! timer-container conj
                                                   (timer (fn [frame-index]
                                                            (when-not (nil? frame-index)
                                                              (reset! running? true)
                                                              (reset! frame-index-atom frame-index)
                                                              (repaint! canv)

                                                              (if (< (inc frame-index)
                                                                     (count frameset))
                                                                (inc frame-index)
                                                                (do
                                                                  (reset! running? false)
                                                                  (reset! frame-index-atom 0)
                                                                  (doseq [timer @timer-container]
                                                                    (.stop timer)
                                                                    (swap! @timer-container rest))
                                                                  nil))))
                                                 :initial-value 0
                                                 :delay @time-per-frame
                                                 :repeats? true))))])])
                                          
                      :divider-location 1/5)))
