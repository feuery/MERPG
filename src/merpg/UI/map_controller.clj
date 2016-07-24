(ns merpg.UI.map-controller
  (:require [merpg.IO.tileset :refer [load-tileset]]
            [merpg.mutable.layers :refer [current-hitlayer current-hitlayer-data]]
            [merpg.UI.render-drawqueue :refer :all]
            [merpg.events.mouse :refer [post-mouse-event!]]
            [merpg.UI.draggable-canvas :refer :all]
            [merpg.2D.core :refer :all]
            [merpg.UI.tool-box :refer :all]
            [merpg.mutable.registry-views :refer [add-rendered-map-watcher remove-rendered-map-watcher rendered-maps]]
            [merpg.mutable.registry :refer [peek-registry]]
            
            [seesaw.core :refer [frame config! listen alert button repaint! border-panel input scrollable canvas]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))


(defn render-hitlayer! [w h]
  (when (realized? current-hitlayer-data)
    (let [hit-layer @current-hitlayer-data]
      (-> (draw-to-surface (image w h)
                       (doseq [[x y] (for [x (range (long (/ w 50)))
                                           y (range (long (/ h 50)))]
                                       [x y])]
                         (with-color 
                          (if (:can-hit? (get-in hit-layer [x y]))
                            "#00FF00"
                            "#FF0000")
                           (Rect (* x 50) (* y 50) 50 50 :fill? true))))
          (set-opacity 110)))))
        

(defn map-surface! []
  (let [selected-map (peek-registry :selected-map)]
    (if (realized? rendered-maps)    
      (let [img (get @rendered-maps selected-map)]
        (if (and (some? img)
                 (= (peek-registry :selected-tool) :hit-tool))
          (draw-to-surface img
                           (Draw (render-hitlayer! (img-width img)
                                                   (img-height)) [0 0]))
          img))
      (image 100 100 :color "#FF0000"))))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  []
  (let [c (draggable-canvas :paint-fn
                            (fn [_ g]
                              (doto g
                                (.drawImage (map-surface!) nil 0 0)
                                (.drawImage @drawqueue nil 0 0)))
                            :surface-provider map-surface!
                            :draggable-fn (fn [e]
                                            (let [[x y] (mouse-location e)]
                                              (post-mouse-event! x y :map-controller :mousemove)))
                            :onmousedown (fn [e]
                                           (let [[x y] (mouse-location e)]
                                             (post-mouse-event! x y :map-controller :mousedown)))
                            :onmouseup (fn [e]
                                         (let [[x y] (mouse-location e)]
                                              (post-mouse-event! x y :map-controller :mouseup))))]
    (scrollable c)))

(defn show [f stuff]
  (config! f :content stuff))
