(ns merpg.UI.map-controller
  (:require [merpg.UI.BindableCanvas :refer :all]
            [merpg.IO.tileset :refer [load-tileset]]
            [merpg.immutable.basic-map-stuff :refer :all]
            [merpg.immutable.map-layer-editing :refer [get-tile]]
            [merpg.mutable.tool :refer :all]
            [merpg.2D.core :refer :all]
            [seesaw.core :refer [frame config! listen alert]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]))

(defn screen->map [coord]
  (long (/ coord 50)))

(defn get-coords [w h step]
  (for [x (range 0 w step)
        y (range 0 h step)]
    [x y]))

(defn map->img [map tileset-list] ;;non-atom
  (draw-to-surface (image (* 50 (width map))
                          (* 50 (height map)))
                   (dotimes [layer (layer-count map)]                     
                     (doseq [[x y :as x-y] (get-coords (* 50 (width map))
                                                       (* 50 (height map)) 50)]
                       (let [tile (get-tile map layer
                                            (long (/ x 50))
                                            (long (/ y 50)))]
                         (Draw (get-in tileset-list [(:tileset tile)
                                                     (:x tile)
                                                     (:y tile)])
                               x-y))))))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  []
  (let [tool-atom (atom {})
        deftool (tool-factory-factory tool-atom)
        map-width (ref 10)
        map-height (ref 10)
        map-data-image (ref (make-map @map-width
                                 @map-height
                                 2))
        tileset-ref (ref [(load-tileset "/Users/feuer2/Dropbox/memapper/tileset.png")])
        
        map-img (image (* @map-width 50)
                       (* @map-height 50))
        map-img (draw-to-surface map-img
                                 (doseq [[x y] (get-coords (* 50 @map-width)
                                                           (* 50 @map-height)
                                                           50)]
                                   (Rect x y 50 50)))
        tilesets (ref [])
        canvas (bindable-canvas map-data-image #(map->img % @tileset-ref))]
    ;; Load tools
    ;; do nothing 'til tileset is loaded
    ;; Allow mouse-dragging to call tools
    
    (listen canvas :mouse-dragged
            (fn [e]
              (alert (str "Klikkasit sitten kohtaa " (-> screen->map
                                                         (map (mouse-location e))
                                                         vec)))))
    canvas))

(defn- show [f stuff]
  (config! f :content stuff))
