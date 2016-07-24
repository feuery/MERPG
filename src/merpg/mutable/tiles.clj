(ns merpg.mutable.tiles
  (:require [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.reagi :refer [editor-stream]]
            [merpg.2D.core :refer :all]
            [reagi.core :as r]
            [schema.core :as s]))

(defn tile [x y tileset rotation]
  (zipmap [:x :y :tileset :rotation]
          [x y tileset rotation]))

(defn tile!
  "Returns id tile is registered with"
  [x y tileset rotation map-x map-y parent-id & {:keys [debug?] :or {debug? false}}]
  (let [id (re/register-element! (zipmap
                                  [:x
                                   :y
                                   :tileset
                                   :rotation
                                   :map-x
                                   :map-y
                                   :parent-id
                                   :type]
                                  [x y tileset rotation map-x map-y parent-id :tile]))]
    (when debug?
      (println "Created a tile with id " id))
    id))

(s/defn ^:always-validate hit-tile!
  "Returns id tile is registered with"
  [can-hit? :- s/Bool
   map-x map-y parent-id]
  (re/register-element!
   (zipmap [:can-hit?
            :map-x :map-y
            :parent-id
            :type]
           [can-hit? map-x map-y parent-id :tile])))

(defn render-tile! [tile]
  (let [{:keys [x y tileset]} tile
        {imgs :images} (re/peek-registry tileset)]
    (get-in imgs [x y])))

(def current-tile-watchers (atom {}))
(defn add-current-tile-watcher [f k]
  (swap! current-tile-watchers assoc k f))

(defn remove-current-tile-watcher [k]
  (swap! current-tile-watchers dissoc k))

(def selected-tile (editor-stream (r/sample 600 re/registry)
                                  (r/filter #(and (coll? %)
                                                  (contains? % :selected-tile)))
                                  (r/map :selected-tile)
                                  (r/map render-tile!)
                                  (r/map (fn [d]
                                           (doseq [[_ func] @current-tile-watchers]
                                             (func))
                                           d))))

