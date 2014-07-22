(ns merpg.mutable.tool ))

;; Tools are called with current map, current tile, coordinates in-which the tool 
;; was used, and the selected layer.
;; Tools are supposed to return the new map... if not, well, swap! might set something garbage as our map.
;; Tool-functions are dynamically searched from this namespace (newui.Tools), and they are also dynamically placed as swap! parameter on the mapview's mouse-dragged event...

(defn tool-factory-factory ; ;)
  "Returns the def-form with which you create the tools, which are put into the map-atom sent here as a parameter"
  [tool-map-atom]
  (fn [tool-name tool-fn]
    (swap! tool-map-atom #(assoc % tool-name tool-fn)))))

(comment
  (deftool :Pen (fn [map current-tile x y layer]
                  (set-tile map layer x y current-tile)))
  
  (deftool :Test (fn [map current-tile x y layer]
                   (println "Käytit testityökalua kohdassa " x ", " y ", " layer)
                   map))

  (deftool :test4 (fn [map current-tile x y layer]
                    (println "Hello world!")))))

