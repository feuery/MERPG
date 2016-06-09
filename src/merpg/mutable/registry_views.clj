(ns merpg.mutable.registry-views
  (:require [reagi.core :as r]
            [seesaw.core :as s]
            [clojure.core.async :as a]
            [clojure.pprint :refer [pprint]]))

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
  (def id1 (merpg.mutable.maps/map! 4 4 3))
  (def id2 (merpg.mutable.maps/map! 2 2 5)))

(def local-registry (r/events))
(def layers-view (->> local-registry
                      ;; registry-to-layer builds up the data in a way that you can refer to layer 0's tile at [1 2] with the form (get-in @layers-view [0 1 2])
                      ;; thus we can't simply (map second), that loads only the metadata
                      (r/map (fn [r]
                               
                               (->> r
                                    (filter #(and
                                              (= (-> % second :type) :layer)
                                              (= (-> % second :subtype) :layer)))
                                    (mapv first)
                                    (mapv #(registry-to-layer @local-registry %)))))))

(defn renderable-layers-of!
  "Returns layers associated with the map-id in a renderable form (with tiles)"
  [map-id]
  ;; TODO we need an ordering to the layers
  ;; and the process of sorting has to be implemented here
  (->> @layers-view
       (filterv #(= map-id
                      (-> %
                          (get-in [0 0])
                          :map-id)))))

;; Eka asia aamulla, rakenna tähän automaaginen järjestys
(defn layer-metadata-of!
  "Returns layer-metadatas associated with the map-id"
  [map-id]
  (->> @local-registry

       (filter #(and
                  (= (-> % second :type) :layer)
                  (= (-> % second :subtype) :layer)
                  (= (-> % second :parent-id) map-id)))
       (mapv second)))
