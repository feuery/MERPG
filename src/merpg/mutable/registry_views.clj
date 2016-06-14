(ns merpg.mutable.registry-views
  (:require [reagi.core :as r]
            [seesaw.core :as s]
            [clojure.core.async :as a]
            [clojure.pprint :refer [pprint]])
  (:import [merpg.java map_renderer]))

(defn sort-by-multiple-keys [col & keys]
  (sort-by #(vec (map % keys)) col))

(defn highest-key [col key]
  (if-not (empty? col)
    (->> col
         (map #(get % key -1))
         (apply max))
    (println "Empty collection")))

(defn registry-to-layer
  [registry layer-id]
  (let [mapid (-> registry
                  (get layer-id)
                  :parent-id)
        tiles (->> registry
                   ;; get relevant tiles
                   (filter #(and (= (-> % second :type) :tile)
                                 (= (-> % second :parent-id) layer-id)))
                   ;; drop ids and assoc map-ids to tiles
                   (map (comp #(assoc % :map-id mapid) second)))]
    (if (empty? tiles)
      ;; Exception so that the tests fail.
      ;; Was it just returning nil, tests would incorrectly succeed
      (throw (Exception. "tiles is empty@registry-to-layer")))
    
    (let [;; sort by map-x map-y for easy partitioning
          tiles (sort-by-multiple-keys tiles :map-x :map-y)
          w (inc (highest-key tiles :map-x))
          h (inc (highest-key tiles :map-y))]
      (->> tiles
           (partition w)
           (mapv vec)))))

(defn layer-ids
  "Skips hit-layer"
  [registry map-id]
  (let [layers (->> registry
                    (filter #(and (= (-> % second :type) :layer)
                                  (= (-> % second :subtype) :layer))))]
    (->> layers
         (map first)
         vec)))



(comment
  ;; Reset registry with these while repling
  (reset! merpg.mutable.registry/registry {})
  (def id1 (merpg.mutable.maps/map! 4 4 1))
  (def id2 (merpg.mutable.maps/map! 2 2 5)))

(def local-registry (r/events))
(def layers-meta (->> local-registry
                      (r/map (fn [r]
                               (->> r
                                    (filter #(and
                                              (= (-> % second :type) :layer)
                                              (= (-> % second :subtype) :layer)))
                                    (sort-by #(-> % second :order))
                                    (mapv second))))))

(def layers-view (->> local-registry
                      ;; registry-to-layer builds up the data in a way that you can refer to layer 0's tile at [1 2] with the form (get-in @layers-view [0 1 2])
                      ;; thus we can't simply (map second), that loads only the metadata
                      (r/map (fn [r]
                               (->> r
                                    (filter #(and
                                              (= (-> % second :type) :layer)
                                              (= (-> % second :subtype) :layer)))
                                    (sort-by #(-> % second :order))
                                    (map first)
                                    (mapv #(registry-to-layer @local-registry %)))))))

(def rendered-maps-watchers (atom {}))
(defn add-rendered-map-watcher [f k]
  (swap! rendered-maps-watchers assoc k f))

(defn remove-rendered-map-watcher [k]
  (swap! rendered-maps-watchers dissoc k))

;; TODO optimize this to render only the selected map
(def rendered-maps
  "This contains all the rendered maps AS A pmapped SEQ - DO NOT USE GET HERE"
  (->> local-registry
       (r/map (fn [r]
                (->> r
                     (filter #(= (-> % second :type) :map))
                     (pmap #(do
                              [(first %) (map_renderer/render (first %))]))
                     (into {}))))
       ;; Side-effecting hack to make it easyish to update the gui
       (r/map (fn [r]
                (doseq [[_ func] @rendered-maps-watchers]
                  (func))
                r))))

(defn renderable-layers-of!
  "Returns layers associated with the map-id in a renderable form (with tiles)"
  [map-id]

  (->> @layers-view
       (filterv #(= map-id
                      (-> %
                          (get-in [0 0])
                          :map-id)))))

(defn layer-metadata-of!
  "Returns layer-metadatas associated with the map-id"
  [map-id]
  (->> @local-registry

       (filter #(and
                  (= (-> % second :type) :layer)
                  (= (-> % second :subtype) :layer)
                  (= (-> % second :parent-id) map-id)))
       (mapv second)))
