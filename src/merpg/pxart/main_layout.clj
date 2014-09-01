(ns merpg.pxart.main-layout ;;jee
  (:require [seesaw.core :refer :all :exclude [width height]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [merpg.immutable.basic-map-stuff :refer [make-thing
                                                     width
                                                     height
                                                     layer-name] :rename {width mapwidth height mapheight}]
            [merpg.UI.map-controller :refer [show drag-location-scrollbar-transformer
                                             make-scrollbar-with-update]]
            [merpg.UI.BindableCanvas :refer [bindable-canvas]]
            [merpg.UI.BindableList :refer [bindable-list]]
            [merpg.util :refer [vec-insert]]
            [merpg.2D.core :refer :all]
            [merpg.pxart.colors :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn max-in-seq [seq]
  (reduce max seq))


(defn get-color-choosers [palette-range current-color-atom]
  (->> palette-range
       (map (fn [color]
              (canvas :background (number->color color)
                      :listen
                      [:mouse-clicked (fn [_]
                                     (reset! current-color-atom color))])))
       vec))

(defn make-frame-general [frame-atom w h palette]
  (-> (max-in-seq palette)
      (make-thing  w h)
      (layer-name (str (count @frame-atom) "th"))))

(defn get-content [image-list-atom f]
  (let [palette-size 4
        palette (range 0 palette-size)
        scroll-X-atom (atom 0)
        scroll-Y-atom (atom 0)

        horizontal-scroll (make-scrollbar-with-update scroll-X-atom)
        vertical-scroll (make-scrollbar-with-update scroll-Y-atom :vertical? true)
        
        W 10
        H 10];;Lowest is black, highest is white

    (println "in get-content")
    
    
    (def make-frame (partial make-frame-general image-list-atom))

    (swap! image-list-atom (comp vec conj) (make-frame W H palette))
    
    (println "class: " (class @image-list-atom))
    (println "image list atom: ")
    (pprint @image-list-atom)
    (def current-image-index (atom 0))
    (def current-image-atom (atom (get @image-list-atom @current-image-index)
                                  :validator (complement nil?)))

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
          _ (println "current-image-atom: " current-image-atom)
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
                  (and (not (nil? @current-image-atom))
                       (< x (mapwidth @current-image-atom))
                       (< y (mapheight @current-image-atom))
                       (swap! current-image-atom assoc-in x-y @current-color-atom)))))
      (left-right-split
       (vertical-panel
        :items ["Colours"
                (grid-panel :columns 2 :items (get-color-choosers palette current-color-atom))

                "Frames"
                (bindable-list image-list-atom current-image-atom :selected-index-atom current-image-index
                               :custom-model-bind layer-name)
                (horizontal-panel :items
                                  [(button :text "Copy current"
                                           :listen
                                           [:action (fn [_]
                                                      (let [current (layer-name @current-image-atom (str (count @image-list-atom) "th"))]
                                                        (swap! image-list-atom vec-insert @current-image-index current)))])
                                   (button :text "Add new"
                                           :listen
                                           [:action (fn [_]
                                                      (swap! image-list-atom vec-insert @current-image-index (make-frame W H palette)))])])
                
                "Selected colour"
                (bindable-canvas current-color-atom #(draw-to-surface (image 50 50)
                                                                      (with-color (number->color %)
                                                                        (Rect 0 0 50 50 :fill? true))))
                (button :text "Clear current frame with current color"
                        :listen
                        [:action (fn [_]
                                   (reset! current-image-atom (make-thing @current-color-atom W H)))])])
       (border-panel :center canv
                     :east vertical-scroll
                     :south
                     (vertical-panel
                      :items [horizontal-scroll
                              (button :listen
                                      [:action (fn [_]
                                                 (dispose! f))])]))))))

(defn show-animation-editor [animations-atom-container current-index]
  (let [;; current-frameset (atom (get @animations-atom-container current-index))
        f (frame :width 800
                 :height 600
                 :visible? true
                 :title "merpg-px-art")]
    (config! f :content (get-content animations-atom-container f))
    ;; (add-watch current-frameset :animation-watcher
    ;;            (fn [_ _ _ new-frameset]
    ;;              (swap! animations-atom-container assoc current-index new-frameset)))
    ))
