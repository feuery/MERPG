(ns merpg.UI.map-controller
  (:require [merpg.UI.BindableCanvas :refer :all]
            [merpg.IO.tileset :refer [load-tileset]]
            [merpg.immutable.basic-map-stuff :refer :all]
            [merpg.immutable.map-layer-editing :refer [get-tile
                                                       set-tile]]
            [merpg.mutable.tool :refer :all]
            [merpg.2D.core :refer :all]
            [merpg.UI.tool-box :refer :all]
            [seesaw.core :refer [frame config! listen alert button repaint!]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]))

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

(defn map->img [Map tileset-list draw-hit-layer?] ;;non-atom
  ;; (println "Drawing hit-layer? " draw-hit-layer?)
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
                                                                   (long (/ y 50)))]
                                                (Draw (get-in tileset-list [(:tileset tile)
                                                                            (:x tile)
                                                                            (:y tile)])
                                                      x-y))))
                           (Draw (set-opacity layer-img opacity) [0 0]))))

                     (when draw-hit-layer?
                       (doseq [[x y :as x-y] (get-coords (* 50 (width Map))
                                                         (* 50 (height Map)) 50)]
                         (let [img (if (get-in (hitdata Map) (map screen->map x-y))
                                     yes
                                     no)]
                           (Draw img x-y)))))
    (draw-to-surface (image 200 100)
                     (Draw "Load a tileset, please" [0 0]))))

(defn default-tools [deftool mouse-map-a]
  (deftool :pen (fn [map current-tile x y layer]
                  ;; (println "using pen" [current-tile x y layer])
                  (set-tile map layer x y current-tile)))
  (deftool :hit-tool (fn [map current-tile x y layer]
                       (if (get-in @mouse-map-a [x y])
                         (hitdata map (set-tile (hitdata map) x y (not (get-in (hitdata map) [x y]))))
                         map))))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  [map-data-image tool-atom current-tool-fn-atom tileset-ref current-tile-ref current-layer-ind-atom selected-tool mouse-down-a? mouse-map-a]
  
  (let [deftool (tool-factory-factory tool-atom mouse-down-a? mouse-map-a)
        map-width  10
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
                                                  (map->img % @tileset-ref (= @selected-tool :hit-tool))))]

    ;; init tools
    (default-tools deftool mouse-map-a)
    (reset! current-tool-fn-atom (:pen @tool-atom))
    
    
    ;; do nothing 'til tileset is loaded
    ;; Allow mouse-dragging to call tools

    ;; [map current-tile x y layer]
    (listen canvas
            :mouse-dragged
            (fn canvas-drag-listener [e]
              (let [[x y :as coords] (-> screen->map
                                         (map (mouse-location e))
                                         vec)
                    tool @current-tool-fn-atom]
                (if-not (get-in @mouse-map-a [x y])
                  (swap! map-data-image tool @current-tile-ref x y @current-layer-ind-atom))))
            :mouse-pressed (fn [_]
                             (swap! mouse-down-a? not))
            :mouse-released (fn [_]
                              (swap! mouse-down-a? not)))
    (add-watch selected-tool :tool-watcher (fn [_ _ _ _]
                                             (repaint! canvas)))
    canvas))

(defn show [f stuff]
  (config! f :content stuff))
