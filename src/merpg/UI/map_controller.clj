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
            [seesaw.core :refer [frame config! listen alert button repaint! border-panel input scrollable]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer :all])
  (:import [javax.swing JScrollBar]
           [merpg.java map_renderer]
           [java.awt.event AdjustmentListener]))

(defn screen->map [coord]
  (long (/ coord 50)))

(defn get-coords [w h step]
  (for [x (range 0 w step)
        y (range 0 h step)]
    [x y]))

(defn to-long [number]
  (try
    (if (number? number)
      number
      (Long/parseLong number))
    (catch ClassCastException ex
        (println "Number " number " (" (class number) ") not converted to long")
        255)))

(defn default-tools [deftool mouse-map-a first-click second-click]
  (def -deftool deftool)
  (deftool :pen (fn [map current-tile x y layer]
                  (locking *out*
                    (println "USING :PEN")
                    (pprint current-tile)
                    (set-tile map layer x y current-tile))))
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
  
  (deftool :fill-box (fn [Map current-tile layer-x layer-y layer]
                       (println "In fill-box?")
                       (if (get-in @mouse-map-a [layer-x layer-y])
                         (if (nil? @first-click)
                           (do
                             (reset! first-click [layer-x layer-y])
                             (println "First-click reset to " [layer-x layer-y])
                             Map)
                           (try
                             (let [second-click [layer-x layer-y]
                                   mutable-map (atom Map)
                                   ys (map second [@first-click second-click])
                                   xs (map first [@first-click second-click])
                                   lower-y (apply min ys)
                                   higher-y (+ (apply max ys) 1)
                                   lower-x (apply min xs)
                                   higher-x (+ (apply max xs) 1)
                                   coordinates (for [x (range lower-x higher-x)
                                                     y (range lower-y higher-y)]
                                                 [x y])]
                               (doseq [[x y :as coord] coordinates]
                                 (swap! mutable-map set-tile layer x y current-tile))
                               (reset! first-click nil)
                               @mutable-map)
                             (catch Exception ex
                               ;; (print-stack-trace ex)
                               nil
                               )))
                         Map)))
  (deftool :reset-filler (fn [Map current-tile layer-x layer-y layer]
                           (reset! first-click nil)
                           Map))
  (deftool :Zonetiles (fn [Map current-tile layer-x layer-y layer]
                        (let [prompt "Name of a function to be called on this tile?"
                              old-fn (-> Map
                                         zonetiles
                                         (get [layer layer-x layer-y]))
                              new-fn (input prompt :value old-fn)]
                          (zonetiles Map
                                     (-> Map zonetiles
                                         (assoc [layer layer-x layer-y] new-fn)))))))

(defn drag-location-scrollbar-transformer [ mouse-coord]
  (let [toret (-> (fn [mouse-pos]
                    (-> mouse-pos
                        (/ 50)
                        double
                        Math/floor
                        int))
                  (map mouse-coord)
                  vec)]
    toret))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  [map-data-image tool-atom current-tool-fn-atom tileset-atom current-tile-ref current-layer-ind-atom selected-tool mouse-down-a? mouse-map-a map-renderer]  

  (def map-event-queue (atom []))
  (def deftool (tool-factory-factory tool-atom mouse-down-a? mouse-map-a map-data-image))

  (def first-click (atom nil))
  (def second-click (atom nil))

  (let [map-width  10
        map-height  10        
        {scroll :scrollable 
         canvas :canvas} (bindable-canvas map-data-image (fn [& _]
                                                 (try
                                                   (println "Rendering")
                                                   (.render map-renderer)
                                                   (catch Exception ex
                                                     (print-stack-trace ex)))) :rest-to-bind [selected-tool])]
    (config! scroll :background :black)

    ;; init tools
    (default-tools deftool mouse-map-a first-click second-click)
    (reset! current-tool-fn-atom (:pen @tool-atom))
    
    
    ;; do nothing 'til tileset is loaded
    ;; Allow mouse-dragging to call tools

    (listen canvas
            :mouse-dragged
            (fn canvas-drag-listener [e]
              (enqueue! map-event-queue (future
                                          (let [[x y :as coords] (-> (mouse-location e)
                                                                     drag-location-scrollbar-transformer)
                                                tool @current-tool-fn-atom]
                                            (when-not (get-in @mouse-map-a [x y])
                                              (if-not (number? (:tileset @current-tile-ref))
                                                (locking *out*
                                                  (println "Current-tile-ref: ")
                                                  (pprint @current-tile-ref)
                                                  (swap! map-data-image tool @current-tile-ref x y @current-layer-ind-atom))
                                                (locking *out*
                                                  (println "(:tileset @current-tile-ref) is numeric"))))))))
            :mouse-pressed (fn [_]
                             (swap! mouse-down-a? not))
            :mouse-released (fn [_]
                              (swap! mouse-down-a? not)))
    scroll))

(defn show [f stuff]
  (config! f :content stuff))
