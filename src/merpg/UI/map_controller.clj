(ns merpg.UI.map-controller
  (:require [merpg.UI.BindableCanvas :refer :all]
            [merpg.IO.tileset :refer [load-tileset]]
            [merpg.immutable.basic-map-stuff :refer :all]
            [merpg.immutable.map-layer-editing :refer [get-tile
                                                       set-tile]]
            [merpg.mutable.tool :refer :all]
            [merpg.2D.core :refer :all]
            [merpg.UI.tool-box :refer :all]
            [merpg.util :refer [abs enqueue! dequeue!]]
            [seesaw.core :refer [frame config! listen alert button repaint! border-panel]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn screen->map [coord]
  (long (/ coord 50)))

(defn get-coords [w h step]
  (for [x (range 0 w step)
        y (range 0 h step)]
    [x y]))

(def yes (set-opacity
          (draw-to-surface (image 50 50)
                           (with-color "#00FF00"
                             (Rect 0 0 50 50 :fill? true))) 200))
(def no (set-opacity
         (draw-to-surface (image 50 50)
                          (with-color "#FF0000"
                            (Rect 0 0 50 50 :fill? true))) 200))

(defn map->img [Map tileset-list draw-hit-layer?
                & {:keys [scroll-coords]
                   :or {scroll-coords [0 0]}}] ;;non-atom
  (println "@map->img scroll-coords: " scroll-coords)
  (if (pos? (count tileset-list))
    (draw-to-surface (image (* 50 (width Map))
                            (* 50 (height Map)))
                     ;; (println "count tileset-list " (count tileset-list))
                     (dotimes [layer (layer-count Map)]
                       (when (-> (get Map layer) layer-visible)
                         (let [layer-img (image (* 50 (width Map))
                                                (* 50 (height Map)))
                               opacity (-> (get Map layer) opacity)]
                           (draw-to-surface layer-img
                                            (doseq [[x y :as x-y] (get-coords (* 50 (width Map))
                                                                              (* 50 (height Map)) 50)]
                                              (let [tile (get-tile Map layer
                                                                   (long (/ x 50))
                                                                   (long (/ y 50)))
                                                    img (-> tileset-list
                                                            (get-in [(:tileset tile)
                                                                     (:x tile)
                                                                     (:y tile)])
                                                            (rotate (* (:rotation tile) 90)))]
                                                (Draw img x-y))))
                           ;; (if (nil? scroll-coords)
                           ;;   (Draw (set-opacity layer-img opacity) [0 0])
                             (Draw (set-opacity layer-img opacity) scroll-coords))));)

                     (when draw-hit-layer?
                       (println "scroll-coords " scroll-coords)
                       (doseq [[x y :as x-y] (get-coords (* 50 (width Map))
                                                         (* 50 (height Map)) 50)]
                         (let [img (if (get-in (hitdata Map) (map screen->map x-y))
                                     yes
                                     no)]
                           (Draw img (vec (map + x-y scroll-coords)))))))
    (draw-to-surface (image 200 100)
                     (Draw "Load a tileset, please" [0 0]))))

(defn default-tools [deftool mouse-map-a]
  (deftool :pen (fn [map current-tile x y layer]
                  ;; (println "using pen" [current-tile x y layer])
                  (set-tile map layer x y current-tile)))
  (deftool :hit-tool (fn [map current-tile x y layer]
                       (if (get-in @mouse-map-a [x y])
                         (hitdata map (set-tile (hitdata map) x y (not (get-in (hitdata map) [x y]))))
                         map)))
  (deftool :rotater (fn [map current-tile layer-x layer-y layer]
                      (if (get-in @mouse-map-a [layer-x layer-y])
                        (let [{x :x y :y tileset :tileset rotation :rotation} (get-tile map layer layer-x layer-y)]
                          (set-tile map layer layer-x layer-y (tile x y tileset (if (> (inc rotation) 3)
                                                                      0
                                                                      (inc rotation)))))
                        map)))

  (let [first-click (atom nil)
        second-click (atom nil)]
    (deftool :fill-box (fn [map current-tile layer-x layer-y layer]
                         (if (nil? @first-click)
                           (do
                             (reset! first-click [layer-x layer-y])
                             map)
                           (if (nil? @second-click)
                             (do
                               (reset! second-click [layer-x layer-y])
                               map)
                             (let [mutable-map (atom map)
                                   ys (map second [@first-click @second-click])
                                   xs (map first [@first-click @second-click])
                                   lower-y (min ys)
                                   higher-y (max ys)
                                   lower-x (min xs)
                                   higher-x (max xs)
                                   coordinates (for [x (range lower-x higher-x)
                                                     y (range lower-y higher-y)]
                                                 [x y])]
                               (doseq [[x y :as coord] coordinates]
                                 ;; set-tile params
                                 ;; [map layer x y tile]
                                 (swap! mutable-map set-tile layer x y current-tile))
                               (reset! first-click nil)
                               (reset! second-click nil)
                               @mutable-map)))))))

(defn make-scrollbar-with-update [scrollbar-val-atom & {:keys [vertical?] :or {vertical? false}}]
  (doto (JScrollBar. (if vertical? JScrollBar/VERTICAL JScrollBar/HORIZONTAL))
    (.addAdjustmentListener
     (proxy [AdjustmentListener] []
       (adjustmentValueChanged [e]
         ;; (when-not (.getValueIsAdjusting e)
           (let [to-screen-coord (comp - (partial * 50))
                 scrollbar-val (.getValue e)]
             (swap! scrollbar-val-atom (fn [_] (to-screen-coord scrollbar-val)))))))));)

(defn drag-location-scrollbar-transformer [ mouse-coord  scroll-coord]
  (let [toret (-> (fn [mouse-pos scroll-pos]
         (-> mouse-pos
             (+ (abs scroll-pos))
             (/ 50)
             double
             Math/floor
             int))
      (map mouse-coord scroll-coord)
      vec)]
    (println "mouse " mouse-coord)
    (println "scroll " scroll-coord)
    (println "toret of drag-location-scrollbar-transformer: " toret)
    toret))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  [map-data-image tool-atom current-tool-fn-atom tileset-ref current-tile-ref current-layer-ind-atom selected-tool mouse-down-a? mouse-map-a]

  (def scroll-X-atom (atom 0))
  (def scroll-Y-atom (atom 0))

  (def map-event-queue (atom []))
  (def deftool (tool-factory-factory tool-atom mouse-down-a? mouse-map-a map-data-image))
  (let [map-width  10
        map-height  10        
        map-img (image (* map-width 50)
                       (* map-height 50))
        map-img (draw-to-surface map-img
                                 (doseq [[x y] (get-coords (* 50 map-width)
                                                           (* 50 map-height)
                                                           50)]
                                   (Rect x y 50 50)))
        canvas (bindable-canvas map-data-image #(do
                                                  ;; (println "drawing hit-layer? " (= @selected-tool :hit-tool))
                                                  (map->img % @tileset-ref (= @selected-tool :hit-tool)
                                                            :scroll-coords [@scroll-X-atom @scroll-Y-atom])))
        horizontal-scroll (make-scrollbar-with-update scroll-X-atom)
        vertical-scroll (make-scrollbar-with-update scroll-Y-atom :vertical? true)]

    ;; init tools
    (default-tools deftool mouse-map-a)
    (reset! current-tool-fn-atom (:pen @tool-atom))
    
    
    ;; do nothing 'til tileset is loaded
    ;; Allow mouse-dragging to call tools

    ;; [map current-tile x y layer]
    (doseq [a [scroll-Y-atom scroll-X-atom]]
      (add-watch a :scroll-repainter (fn [_ _ _ _]
                                       (repaint! canvas))))
    (listen canvas
            :mouse-dragged
            (fn canvas-drag-listener [e]
              (enqueue! map-event-queue (future
                                          (let [[x y :as coords] (-> (mouse-location e)
                                                                     (drag-location-scrollbar-transformer [@scroll-X-atom @scroll-Y-atom]))
                                                tool @current-tool-fn-atom]
                                            (when-not (get-in @mouse-map-a [x y])
                                              (println "Doing stupid stuff")
                                              (swap! map-data-image tool @current-tile-ref x y @current-layer-ind-atom))))))
            :mouse-pressed (fn [_]
                             (swap! mouse-down-a? not))
            :mouse-released (fn [_]
                              (swap! mouse-down-a? not)))
    (add-watch selected-tool :tool-watcher (fn [_ _ _ _]
                                             (repaint! canvas)))
    (border-panel :south horizontal-scroll
                  :east vertical-scroll
                  :center canvas)))

(defn show [f stuff]
  (config! f :content stuff))
