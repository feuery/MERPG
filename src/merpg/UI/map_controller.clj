(ns merpg.UI.map-controller
  (:require [merpg.UI.BindableCanvas :refer :all]
            [merpg.IO.tileset :refer [load-tileset]]
            [merpg.immutable.basic-map-stuff :refer :all]
            [merpg.immutable.map-layer-editing :refer [get-tile
                                                       set-tile]]
            [merpg.mutable.tool :refer :all]
            [merpg.2D.core :refer :all]
            [merpg.UI.tool-box :refer :all]
            [seesaw.core :refer [frame config! listen alert button]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]))

(defn screen->map [coord]
  (long (/ coord 50)))

(defn get-coords [w h step]
  (for [x (range 0 w step)
        y (range 0 h step)]
    [x y]))

(defn map->img [map tileset-list] ;;non-atom
  (if (pos? (count tileset-list))
    (draw-to-surface (image (* 50 (width map))
                            (* 50 (height map)))
                     (println "count tileset-list " (count tileset-list))
                     (dotimes [layer (layer-count map)]                     
                       (doseq [[x y :as x-y] (get-coords (* 50 (width map))
                                                         (* 50 (height map)) 50)]
                         (let [tile (get-tile map layer
                                              (long (/ x 50))
                                              (long (/ y 50)))]
                           (Draw (get-in tileset-list [(:tileset tile)
                                                       (:x tile)
                                                       (:y tile)])
                                 x-y)))))
    (draw-to-surface (image 200 100)
                     (Draw "Load a tileset, please" [0 0]))))

(defn default-tools [deftool]
  (deftool :pen (fn [map current-tile x y layer]
                  (set-tile map layer x y current-tile))))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  [map-data-image tool-atom current-tool-fn-atom tileset-ref]
  
  (let [deftool (tool-factory-factory tool-atom)
        map-width  10
        map-height  10
        _ (comment map-data-image (ref (make-map map-width
                                                 map-height
                                                 2)))
        
        map-img (image (* map-width 50)
                       (* map-height 50))
        map-img (draw-to-surface map-img
                                 (doseq [[x y] (get-coords (* 50 map-width)
                                                           (* 50 map-height)
                                                           50)]
                                   (Rect x y 50 50)))
        canvas (bindable-canvas map-data-image #(map->img % @tileset-ref))]

    ;; init tools
    (default-tools deftool)
    (reset! current-tool-fn-atom (:pen @tool-atom))
    (tool-frame! tool-atom current-tool-fn-atom)
    
    ;; do nothing 'til tileset is loaded
    ;; Allow mouse-dragging to call tools

    ;; [map current-tile x y layer]
    (listen canvas :mouse-dragged
            (fn canvas-drag-listener [e]
              (let [[x y :as coords] (-> screen->map
                                         (map (mouse-location e))
                                         vec)
                    tool @current-tool-fn-atom]
                (dosync
                 (alter map-data-image tool (tile 1 1 0 1) x y (dec (layer-count @map-data-image)))))))
    canvas))

(defn show [f stuff]
  (config! f :content stuff))
