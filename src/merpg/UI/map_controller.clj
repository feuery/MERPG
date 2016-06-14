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
            [merpg.mutable.registry-views :refer [add-rendered-map-watcher remove-rendered-map-watcher rendered-maps]]
            [merpg.mutable.registry :refer [peek-registry]]
            
            [seesaw.core :refer [frame config! listen alert button repaint! border-panel input scrollable canvas]]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn default-tools [& _;; deftool mouse-map-qa first-click second-click
                     ]
  (throw (Exception. "Default tools is under construction"))
  (comment
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
                                           (assoc [layer layer-x layer-y] new-fn))))))))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  []
  (let [c (canvas :paint (fn [_ g]
                           ;; TODO fix to support multiple maps :D
                           (let [selected-map (peek-registry :selected-map)]
                             (if (realized? rendered-maps)
                               (.drawImage g (get @rendered-maps selected-map) nil 0 0)
                               (.drawImage g (image 100 100 :color "#FF0000") nil 0 0)))))]
    (remove-rendered-map-watcher :map-controller)
    (add-rendered-map-watcher #(do
                                 ;; (println "Repainting canvas")
                                 (repaint! c))
                                 :map-controller)
    (scrollable c)))

(defn show [f stuff]
  (config! f :content stuff))
