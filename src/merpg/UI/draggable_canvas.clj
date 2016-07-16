(ns merpg.UI.draggable-canvas
  (:require [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [merpg.2D.core :as dd]
            [merpg.events.mouse :refer [current-mouse-location]]))

(defn draggable-canvas [& {:keys [paint-fn draggable-fn surface-provider
                                  onmousedown onmouseup]
                           :or {paint-fn identity
                                draggable-fn identity
                                surface-provider (constantly (dd/image 50 50))
                                onmousedown identity
                                onmouseup identity}}]
  (let [mouse-visited-map (atom nil)]
    (canvas :paint
            paint-fn
            :listen
            [:mouse-pressed
             (fn [e]
               (let [surface (surface-provider)
                     [w h] (mapv #(/ % 50) [(dd/img-width surface)
                                            (dd/img-height surface)])]
                 (reset! mouse-visited-map (->> false
                                                (repeat h)
                                                vec
                                                (repeat w)
                                                vec))
                 (onmousedown e)))
             :mouse-dragged (fn [e]
                              (let [[x-pxl y-pxl :as mouse-coord] (mouse-location e)
                                    [x-tile y-tile] [(long (/ x-pxl 50))
                                                     (long (/ y-pxl 50))]]
                                (when-not (get-in @mouse-visited-map [x-tile y-tile])
                                  (reset! current-mouse-location mouse-coord)
                                  (draggable-fn e)
                                  (swap! mouse-visited-map update-in [x-tile y-tile] not))))
             :mouse-released #(onmouseup %)])))
