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
            [seesaw.core :refer [frame config! listen alert button repaint! border-panel input]]
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

(def yes (set-opacity
          (draw-to-surface (image 50 50)
                           (with-color "#00FF00"
                             (Rect 0 0 50 50 :fill? true))) 200))
(def no (set-opacity
         (draw-to-surface (image 50 50)
                          (with-color "#FF0000"
                            (Rect 0 0 50 50 :fill? true))) 200))
(defn to-long [number]
  (try
    (if (number? number)
      number
      (Long/parseLong number))
    (catch ClassCastException ex
        (println "Number " number " (" (class number) ") not converted to long")
        255)))

(defn map->img [Map tileset-list draw-hit-layer? first-click second-click
                & {:keys [scroll-coords]
                   :or {scroll-coords [0 0]}}] ;;non-atom
  (println "@map->img scroll-coords: " scroll-coords)
  (if (pos? (count tileset-list))
    (draw-to-surface (image (* 50 (width Map))
                            (* 50 (height Map)))
                     ;;Draw the tiles
                     (dotimes [layer (layer-count Map)]
                       (when (-> (get Map layer) layer-visible)
                         (let [layer-img (image (* 50 (width Map))
                                                (* 50 (height Map)))
                               opacity (-> (get Map layer) opacity to-long)]
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

                                                ;; #break
                                                (if-not (nil? img)
                                                  (Draw img x-y)
                                                  (do
                                                    (println "Img is nil at map->img")
                                                    (println "This is to be expected for map is an atom, not a ref updated in a transaction")
                                                    (def -tileset-list tileset-list)
                                                    (def -tile tile)
                                                    (def -map Map)
                                                    (def -layer layer)
                                                    (def -img img))))))
                             (Draw (set-opacity layer-img opacity) scroll-coords))))
                     ;; Draw hit-thingy
                     (when draw-hit-layer?
                       (println "scroll-coords " scroll-coords)
                       (doseq [[x y :as x-y] (get-coords (* 50 (width Map))
                                                         (* 50 (height Map)) 50)]
                         (let [img (if (get-in (hitdata Map) (map screen->map x-y))
                                     yes
                                     no)]
                           (Draw img (vec (map + x-y scroll-coords))))))

                     ;;Fill-tool's rendering
                     (with-color "#0000FF"
                       (doseq [[x y] (->> [@first-click @second-click]
                                          (filter (complement nil?))
                                          (map #(map (partial * 50) %)))]
                         (Rect x y 50 50))))
    (draw-to-surface (image 200 100)
                     (Draw "Load a tileset, please" [0 0]))))

(defn default-tools [deftool mouse-map-a first-click second-click]
  (def -deftool deftool)
  (deftool :pen (fn [map current-tile x y layer]
                  ;; (println "using pen" [current-tile x y layer])
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
                               (print-stack-trace ex))))
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

(defn make-scrollbar-with-update [scrollbar-val-atom & {:keys [vertical?
                                                               on-adjustment] :or {vertical? false
                                                                                   on-adjustment (fn [_] )}}]
  (doto (JScrollBar. (if vertical? JScrollBar/VERTICAL JScrollBar/HORIZONTAL))
    (.addAdjustmentListener
     (proxy [AdjustmentListener] []
       (adjustmentValueChanged [e]
         ;; (when-not (.getValueIsAdjusting e)
         (let [to-screen-coord (comp - (partial * 50))
               scrollbar-val (.getValue e)]
           (reset! scrollbar-val-atom (to-screen-coord scrollbar-val))
           (on-adjustment scrollbar-val)))))));)

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
    ;; (println "mouse " mouse-coord)
    ;; (println "scroll " scroll-coord)
    ;; (println "toret of drag-location-scrollbar-transformer: " toret)
    toret))

(defn map-controller
  "Returns the mainview, on which we can edit the map"
  [map-data-image tool-atom current-tool-fn-atom tileset-ref current-tile-ref current-layer-ind-atom selected-tool mouse-down-a? mouse-map-a]

  (def scroll-X-atom (atom 0))
  (def scroll-Y-atom (atom 0))

  (def map-event-queue (atom []))
  (def deftool (tool-factory-factory tool-atom mouse-down-a? mouse-map-a map-data-image))

  (def first-click (atom nil))
  (def second-click (atom nil))

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
                                                  (map->img % @tileset-ref (= @selected-tool :hit-tool) first-click second-click
                                                            :scroll-coords [@scroll-X-atom @scroll-Y-atom])))
        horizontal-scroll (make-scrollbar-with-update scroll-X-atom
                                                      :on-adjustment (fn [_]
                                                                       ;; noopping on map-data-image generates a notification on the atom
                                                                       ;; and that refreshes the agent that renders our map
                                                                       ;; which generates a notification for our canvas to draw our new
                                                                       ;; map
                                                                       (swap! map-data-image #(set-tile % 0 0 0
                                                                                                        (get-tile % 0 0 0)))))
        vertical-scroll (make-scrollbar-with-update scroll-Y-atom :vertical? true
                                                    :on-adjustment (fn [_]
                                                                     ;; In other words, sorry for this ugly hack :D
                                                                     (swap! map-data-image #(set-tile % 0 0 0
                                                                                                      (get-tile % 0 0 0)))))]

    ;; init tools
    (default-tools deftool mouse-map-a first-click second-click)
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
    (add-watch selected-tool :tool-watcher (fn [_ _ _ _]
                                             (repaint! canvas)))
    (border-panel :south horizontal-scroll
                  :east vertical-scroll
                  :center canvas)))

(defn show [f stuff]
  (config! f :content stuff))
