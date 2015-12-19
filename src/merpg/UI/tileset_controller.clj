(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.map-controller :refer [screen->map]]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.immutable.basic-map-stuff :refer [tile]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.pprint :refer :all])
  (:import [javax.swing JScrollBar]
           [java.awt.event AdjustmentListener]))

(defn make-scrollbar-with-update [scrollbar-val-atom & {:keys [vertical?
                                                               on-adjustment] :or {vertical? false
                                                                                   on-adjustment (fn [_] )}}]
  (doto (JScrollBar. (if vertical? JScrollBar/VERTICAL JScrollBar/HORIZONTAL))
                     ;; @TODO Implement here some auto-updating maths on how much of the map our current view can show)
    (.addAdjustmentListener
     (proxy [AdjustmentListener] []
       (adjustmentValueChanged [e]
         ;; (when-not (.getValueIsAdjusting e)
         (let [to-screen-coord (comp - (partial * 50))
               scrollbar-val (.getValue e)]
           (reset! scrollbar-val-atom (to-screen-coord scrollbar-val))
           (on-adjustment scrollbar-val)))))))

(defn tileset-controller [tileset-ref current-tileset-index-ref current-tile-ref]  
  (let [scrollX (atom 0)
        scrollY (atom 0)
        
        canvas (bindable-canvas current-tileset-index-ref
                                #(let [toret (tileset-to-img (get @tileset-ref %))]
                                   (locking *out*
                                     (println "Tileset to draw:")
                                     (pprint toret))
                                   (draw-to-surface (image (img-width toret)
                                                           (img-height toret))
                                                    (Draw toret [@scrollX @scrollY])))
                                
                                ;; :rest-to-bind [tileset-ref]
                                )
        {refresh-fn :refresh-fn} (meta canvas)]
    (add-watch scrollX :asd (fn [_ _ _ new]
                              (println "ScrollX: " new)))
    
    (listen canvas :mouse-dragged (fn [e]
                                    (let [[x y :as coords] (-> screen->map
                                                               (map (mouse-location e))
                                                               vec)]
                                      (locking *out*
                                        (try                                        
                                          (dosync
                                           (println "ref-setting current-tile-ref with " @current-tileset-index-ref)
                                           (ref-set current-tile-ref (tile x y @current-tileset-index-ref 0)))
                                          (catch Exception ex
                                            (println ex)))))))
    (config! canvas :background :red)
    (border-panel :west (make-scrollbar-with-update scrollX :vertical? true
                                                    :on-adjustment (fn [_]
                                                                     (refresh-fn)))
                  :south (make-scrollbar-with-update scrollY :on-adjustment
                                                     (fn [_]
                                                       (refresh-fn)))
                  :center canvas)))
