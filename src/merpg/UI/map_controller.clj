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
                           (with-color "#000000"
                             (Rect 0 0 50 50 :fill? true))) 200))
(def no (set-opacity
         (draw-to-surface (image 50 50)
                          (with-color "#FFFFFF"
                            (Rect 0 0 50 50 :fill? true))) 200))

(defn map->img [map tileset-list draw-hit-layer?] ;;non-atom
  ;; (println "Drawing hit-layer? " draw-hit-layer?)
  (if (pos? (count tileset-list))
    (draw-to-surface (image (* 50 (width map))
                            (* 50 (height map)))
                     ;; (println "count tileset-list " (count tileset-list))
                     (dotimes [layer (layer-count map)]
                       (when (-> (get map layer) layer-visible)
                         (let [layer-img (image (* 50 (width map))
                                                (* 50 (height map)))
                               opacity (-> (get map layer) opacity)]
                           (draw-to-surface layer-img
                                            (doseq [[x y :as x-y] (get-coords (* 50 (width map))
                                                                              (* 50 (height map)) 50)]
                                              (let [tile (get-tile map layer
                                                                   (long (/ x 50))
                                                                   (long (/ y 50)))]
                                                (Draw (get-in tileset-list [(:tileset tile)
                                                                            (:x tile)
                                                                            (:y tile)])
                                                      x-y))))
                           (Draw (set-opacity layer-img opacity) [0 0]))))

                     (when draw-hit-layer?
                       ;; (println "The hitdata: " (hitdata map))
                       
                       ;; (println "at 2 2: " (get-in (hitdata map) [2 2]))
                       (doseq [[x y :as x-y] (get-coords (* 50 (width map))
                                                         (* 50 (height map)) 50)]
                         (let [img (if (get-in (hitdata map) [x y])
                                     yes
                                     no)]
                           (Draw img x-y)))
                       (Draw "Hitdata active!" [200 200])))
    (draw-to-surface (image 200 100)
                     (Draw "Load a tileset, please" [0 0]))))

(defn default-tools [deftool]
  (deftool :pen (fn [map current-tile x y layer]
                  ;; (println "using pen" [current-tile x y layer])
                  (set-tile map layer x y current-tile)))
  (deftool :hit-tool (fn [map current-tile x y layer]
                       (println "new hitadata "
                                (hitdata (hitdata map (assoc-in (hitdata map) [x y] (not (get-in (hitdata map) [x y]))))))
                       (hitdata map (assoc-in (hitdata map) [x y] (not (get-in (hitdata map) [x y]))))
                       
                       )))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  [map-data-image tool-atom current-tool-fn-atom tileset-ref current-tile-ref current-layer-ind-atom selected-tool]
  
  (let [deftool (tool-factory-factory tool-atom)
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
    (default-tools deftool)
    (reset! current-tool-fn-atom (:pen @tool-atom))
    
    
    ;; do nothing 'til tileset is loaded
    ;; Allow mouse-dragging to call tools

    ;; [map current-tile x y layer]
    (listen canvas :mouse-dragged
            (fn canvas-drag-listener [e]
              (let [[x y :as coords] (-> screen->map
                                         (map (mouse-location e))
                                         vec)
                    tool @current-tool-fn-atom]
                ;; (println "old hitdata: " (hitdata @map-data-image))
                (swap! map-data-image tool @current-tile-ref x y @current-layer-ind-atom)
                ;; (println "new hitdata: " (hitdata @map-data-image))
                )))
    (add-watch selected-tool :tool-watcher (fn [_ _ _ _]
                                             (repaint! canvas)))
    canvas))

(defn show [f stuff]
  (config! f :content stuff))
