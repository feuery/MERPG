(ns merpg.UI.draggable-canvas
  (:require [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [merpg.2D.core :as dd]))

(defn draggable-canvas [& {:keys [paint-fn draggable-fn surface-provider]
                           :or [paint-fn identity
                                draggable-fn identity
                                surface-provider (constantly (dd/image 50 50))]}]
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
                                                vec))))
             :mouse-dragged (fn [e]
                              (let [[x-pxl y-pxl] (mouse-location e)
                                    [x-tile y-tile] [(long (/ x-pxl 50))
                                                     (long (/ y-pxl 50))]]
                                (when-not (get-in @mouse-visited-map [x-tile y-tile])
                                  (draggable-fn e)
                                  (swap! mouse-visited-map update-in [x-tile y-tile] not))))])))
