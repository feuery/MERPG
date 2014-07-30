(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.map-controller :refer [screen->map]]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.immutable.basic-map-stuff :refer [tile]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]))

(defn tileset-controller [tileset-ref current-tileset-index-ref current-tile-ref]
  (let [canvas (bindable-canvas current-tileset-index-ref
                                #(tileset-to-img (get @tileset-ref %))
                                ;; :rest-to-bind [tileset-ref]
                                )]
    (listen canvas :mouse-dragged (fn [e]
                                    (let [[x y :as coords] (-> screen->map
                                                               (map (mouse-location e))
                                                               vec)]
                                      (dosync
                                       (ref-set current-tile-ref (tile x y @current-tileset-index-ref 0))))))
    (config! canvas :background :red)
    canvas))
