(ns merpg.mutable.tool
  (:require [merpg.immutable.basic-map-stuff :refer [make-bool-layer width height
                                                     width height]]))


;; Tools are called with current map, current tile, coordinates in-which the tool 
;; was used, and the selected layer.
;; Tools are supposed to return the new map... if not, well, swap! might set something garbage as our map.
;; Tool-functions are dynamically searched from this namespace (newui.Tools), and they are also dynamically placed as swap! parameter on the mapview's mouse-dragged event...

(defn tool-factory-factory ; ;)
  "Returns the def-form with which you create the tools, which are put into the map-atom sent here as a parameter"
  [tool-map-atom mouse-down-a? mouse-map-a current-map-atom]
  (add-watch mouse-down-a? :mouse-recorder (fn [_ _ _ mouse-down]
                             (let [curmap @current-map-atom]
                               (reset! mouse-map-a (make-bool-layer (width curmap)
                                                                    (height curmap)
                                                                    :default-value false)))))
  (let [mouse-move-recorder (fn [tool-fn map current-tile x y layer]
                              (when (and (< x (width map))
                                         (< y (height map)))
                                (println "coords at mouse-move-recorder " [x y])
                                (swap! mouse-map-a assoc-in [x y] true)
                                (tool-fn map current-tile x y layer)))]
    (fn [tool-name tool-fn]
      (swap! tool-map-atom #(assoc % tool-name (partial mouse-move-recorder tool-fn))))))

(comment
  (deftool :Pen (fn [map current-tile x y layer]
                  (set-tile map layer x y current-tile)))
  
  (deftool :Test (fn [map current-tile x y layer]
                   (println "Käytit testityökalua kohdassa " x ", " y ", " layer)
                   map))

  (deftool :test4 (fn [map current-tile x y layer]
                    (println "Hello world!"))))
