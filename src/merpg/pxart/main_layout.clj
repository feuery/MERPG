(ns merpg.px-art.main-layout
  (:require [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [merpg.immutable.basic-map-stuff :refer [make-thing width height] :rename {width mapwidth make-scrollbar-with-update mapheight}]
            [merpg.UI.map-controller :refer [show drag-location-scrollbar-transformer
                                             make-scrollbar-with-update]]
            [merpg.UI.BindableCanvas :refer [bindable-canvas]]
            [merpg.2D.core :refer :all]))

(def f (frame :width 800
              :height 600
              :visible? true
              :title "merpg-px-art"))

(defn max-in-seq [seq]
  (reduce max seq))

(defn number->color [n]
  {:pre [(<= 0 n 3)]}
  (case n
    0 "#000000"
    1 "#444444"
    2 "#888888"
    3 "#FFFFFF"
    :else "#FF0000"))

(defn get-color-choosers [palette-range current-color-atom]
  (->> palette-range
       (map (fn [color]
              (canvas :background (number->color color)
                      :listen
                      [:mouse-clicked (fn [_]
                                     (reset! current-color-atom color))])))
       vec))

(defn get-content []  
  (let [palette-size 4
        palette (range 0 palette-size)
        scroll-X-atom (atom 0)
        scroll-Y-atom (atom 0)

        horizontal-scroll (make-scrollbar-with-update scroll-X-atom)
        vertical-scroll (make-scrollbar-with-update scroll-Y-atom :vertical? true)
        
        W 10
        H 10];;Lowest is black, highest is white
    (def image-list-atom (atom [(make-thing (max-in-seq palette) W H)]))
    (def current-image-index (atom 0))
    (def current-image-atom (atom (get @image-list-atom @current-image-index)))

    (def current-color-atom (atom 0 :validator #(< -1 % palette-size)))

    (def index-updating-atom (atom false))
    
    (add-watch current-image-index :index-watch (fn [_ _ _ new]
                                                  (try
                                                    (swap! index-updating-atom not)
                                                    (reset! current-image-atom (get @image-list-atom new))
                                                    (finally (swap! index-updating-atom not)))))
    
    (add-watch current-image-atom :current-updater
               (fn [_ _ _ new-img]
                 (when-not @index-updating-atom
                   (swap! image-list-atom assoc @current-image-index new-img))))
    
    (let [img (image (* 50 W)
                     (* 50 H))
          canv (bindable-canvas current-image-atom (fn [img-data]
                                                (draw-to-surface img
                                                                 (let [w (mapwidth img-data)
                                                                       h (mapheight img-data)]
                                                                 (dotimes [x w]
                                                                   (dotimes [y h]
                                                                     (with-color (number->color (get-in img-data [x y]))
                                                                       (Rect
                                                                        (+ (* x 50) @scroll-X-atom)
                                                                        (+ (* y 50) @scroll-Y-atom) 50 50 :fill? true))
                                                                     (with-color "#FF0000"
                                                                       (Line 0 (* y 50) (* w 50) (* y 50))
                                                                       (Line (* x 50) 0 (* x 50) (* h 50)))))))))]
      (listen canv :mouse-dragged
              (fn [e]
                (let [[x y :as x-y] (-> (mouse-location e)
                              (drag-location-scrollbar-transformer [@scroll-X-atom @scroll-Y-atom]))]
                  (and (< x (mapwidth @current-image-atom))
                       (< y (mapheight @current-image-atom))
                  (swap! current-image-atom assoc-in x-y @current-color-atom)))))
      (left-right-split
       (vertical-panel
        :items ["Colours"
                (grid-panel :columns 2 :items (get-color-choosers palette current-color-atom))
                "Selected colour"
                (bindable-canvas current-color-atom #(draw-to-surface (image 50 50)
                                                                      (with-color (number->color %)
                                                                        (Rect 0 0 50 50 :fill? true))))
                (button :text "Clear with current color"
                        :listen
                        [:action (fn [_]
                                   (reset! current-image-atom (make-thing @current-color-atom W H)))])])
       (border-panel :center canv
                     :east vertical-scroll
                     :south horizontal-scroll)))))
