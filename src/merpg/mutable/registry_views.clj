(ns merpg.mutable.registry-views
  (:require [reagi.core :as r]
            [seesaw.core :as s]
            [clojure.core.async :as a]
            [clojure.pprint :refer [pprint]]
            [merpg.2D.core :refer :all]
            [merpg.reagi :refer [editor-stream]])
  (:import [merpg.java map_renderer]))

(defn highest-key [col key]
  (if-not (empty? col)
    (->> col
         (map #(get % key -1))
         (apply max))
    (println "Empty collection")))

;; a clone of the merpg.mutable.registry/registry we can't use because of the moronic don't depend cyclically - rule
(def local-registry-atom (atom {}))

(defn layer-ids
  "Skips hit-layer"
  [registry map-id]
  (let [layers (->> registry
                    (filter #(and (= (-> % second :type) :layer)
                                  (= (-> % second :subtype) :layer))))]
    (->> layers
         (map first)
         vec)))



(def layers-meta (editor-stream (r/sample 600 r/time)
                                (r/map (fn [_]
                                         (->> @local-registry-atom
                                              (filter #(and
                                                        (= (-> % second :type) :layer)
                                                        (= (-> % second :subtype) :layer)))
                                              (sort-by #(-> % second :order))
                                              (mapv second))))))

(def rendered-maps-watchers (atom {}))
(defn add-rendered-map-watcher [f k]
  (swap! rendered-maps-watchers assoc k f))

(defn remove-rendered-map-watcher [k]
  (swap! rendered-maps-watchers dissoc k))

(defn- query [fun registry]
  (->> registry
       (filter #(fun (-> % second)))))

(defn registry->rendered-map [map-id r]
  (->> r
       ;; get layers
       (query #(and (= (:type %) :layer)
                    (= (:subtype %) :layer)
                    (= (:parent-id %) map-id)))
       ;; order them
       (sort-by #(-> % second :order))
       (into {})
       vals
       ;; async render their layers and assoc to map-id
       (pmap (fn [{:keys [parent-id
                          id] :as mitvit}]
               (if-let [rendered-layer (map_renderer/render parent-id id)]
                 rendered-layer
                 (locking *out*
                   (println "Rendering layer on " [parent-id id] " failed")
                   (pprint mitvit)))))
       
       ;; reduce all the layer-surfaces to a single surface
       (reduce (fn [final-surface layer-surface]
                 (draw-to-surface final-surface
                                  (Draw layer-surface [0 0]))))))

(defn registry->rendered-maps [r]
  (->> r
       ;; all them maps 
       (filter #(= (-> % second :type) :map))
       ;; get map's layers and sort them correctly
       (map (fn [[mapid _]]
              [mapid (->> r
                          (filter
                           #(and (-> % second :parent-id (= mapid))
                                 (-> % second :subtype (= :layer))))
                          (sort-by #(-> % second :order))
                          (mapv first))]))
       ;; async render their layers and assoc to map-id
       (map (fn [[map-id layer-ids]]
              [map-id (pmap (fn [layer-id]
                              (map_renderer/render map-id layer-id)) layer-ids)]))
       ;; reduce all the layer-surfaces to a single surface
       (map (fn [[map-id layer-surfaces]]
              (if (and (some? layer-surfaces)
                       (some? (first layer-surfaces)))
                (let [w (img-width (first layer-surfaces))
                      h (img-height (first layer-surfaces))]
                  [map-id (reduce (fn [map-surface layer-surface]
                                    (if (some? layer-surface)
                                      (draw-to-surface map-surface
                                                       (Draw layer-surface [0 0]))
                                      map-surface))
                                  (image w h)
                                  layer-surfaces)]))))
       (into {})))
  
;; TODO optimize this to render only the selected map
(def rendered-maps 
  "This contains all the rendered maps AS A pmapped SEQ - DO NOT USE GET HERE"
  (editor-stream (r/sample 25 local-registry-atom)
                 (r/map registry->rendered-maps)
                 ;; Side-effecting hack to make it easyish to update the gui
                 (r/map (fn [r]
                          (doseq [[_ func] @rendered-maps-watchers]
                            (func))
                          r))))

(defn layer-metadata-of!
  "Returns layer-metadatas associated with the map-id"
  [map-id]
  (->> @local-registry-atom
       (filter #(and
                  (= (-> % second :type) :layer)
                  (= (-> % second :subtype) :layer)
                  (= (-> % second :parent-id) map-id)))
       (mapv second)))
