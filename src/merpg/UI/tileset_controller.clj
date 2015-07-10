(ns merpg.UI.tileset-controller
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.map-controller :refer [screen->map]]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.immutable.basic-map-stuff :refer [tile]]
            [merpg.IO.tileset :refer [tileset-to-img]]
            [seesaw.core :refer :all]
            [seesaw.mouse :refer [location] :rename {location mouse-location}]
            [clojure.pprint :refer :all]))

(defn tileset-controller [tileset-ref current-tileset-index-ref current-tile-ref]
  (let [canvas (bindable-canvas current-tileset-index-ref
                                #(let [toret (tileset-to-img (get @tileset-ref %))]
                                   (locking *out*
                                     (println "Tileset to draw:")
                                     (pprint toret))
                                   toret)
                                ;; :rest-to-bind [tileset-ref]
                                )]
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
    canvas))
