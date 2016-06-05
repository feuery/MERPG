(ns merpg.mutable.tiles
  (:require [merpg.mutable.registry :as r]
            [schema.core :as s]))

(defn tile!
  "Returns id tile is registered with"
  [x y tileset rotation map-x map-y parent-id]
  (r/register-element! (zipmap
                        [:x
                         :y
                         :tileset
                         :rotation
                         :map-x
                         :map-y
                         :parent-id
                         :type]
                        [x y tileset rotation map-x map-y parent-id :tile])))

(s/defn ^:always-validate hit-tile!
  "Returns id tile is registered with"
  [can-hit? :- s/Bool
   map-x map-y parent-id]
  (r/register-element!
   (zipmap [:can-hit?
            :map-x :map-y
            :parent-id]
           [can-hit? map-x map-y parent-id])))

