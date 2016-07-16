(ns merpg.mutable.sprites
  (:require [merpg.mutable.registry :as re]
            [merpg.UI.events :as e]
            [merpg.2D.core :refer :all]

            [reagi.core :as r]))

(defn static-sprite! [map-id path]
  (let [sprites-per-map (count (re/query! #(and (= (:type %) :sprite)
                                                (= (:parent-id %) map-id))))]
    (e/allow-events
     (re/register-element! {:name "New sprite"
                            :type :sprite
                            :subtype :static
                            :order sprites-per-map
                            :parent-id map-id
                            :x 0
                            :y 0
                            :surface (image path)}))))
