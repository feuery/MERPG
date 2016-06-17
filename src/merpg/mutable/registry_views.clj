(ns merpg.mutable.registry-views
  (:require [reagi.core :as r]
            [seesaw.core :as s]
            [clojure.core.async :as a]
            [clojure.pprint :refer [pprint]]
            [merpg.2D.core :refer :all])
  (:import [merpg.java map_renderer]))

(defn sort-by-multiple-keys [col & keys]
  (sort-by #(vec (map % keys)) col))

(defn highest-key [col key]
  (if-not (empty? col)
    (->> col
         (map #(get % key -1))
         (apply max))
    (println "Empty collection")))

(def local-registry (r/events))

(defn registry-to-layer
  [registry layer-id]
  (let [mapid (-> registry
                  (get layer-id)
                  :parent-id)
        meta (get @local-registry layer-id)
        tiles (->> registry
                   ;; get relevant tiles
                   (filter #(and (= (-> % second :type) :tile)
                                 (= (-> % second :parent-id) layer-id)))
                   ;; assoc map-ids their own ids to tiles
                   (map #(update % 1 assoc
                                 :map-id mapid
                                 :tile-id (first %)
                                 :meta meta))
                   (map second))]
    (when (empty? tiles)
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

(def layers-meta (->> local-registry
                      (r/map (fn [r]
                               (->> r
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

;; TODO optimize this to render only the selected map
(def rendered-maps 
  "This contains all the rendered maps AS A pmapped SEQ - DO NOT USE GET HERE"
  (->> local-registry
       (r/map (fn [r]
                (->> r
                     (filter #(= (-> % second :type) :map))
                     (map (fn [[mapid _]]
                            ;; [map-id ;;layer-ids]
                            [mapid (->> @local-registry
                                        (filter
                                         #(and (-> % second :parent-id (= mapid))
                                               (-> % second :subtype (= :layer))))
                                        (sort-by #(-> % second :order))
                                        (mapv first))]))
                     (map (fn [[map-id layer-ids]]
                            [map-id (pmap (fn [layer-id]
                                            ;; (image 50 50 :color "#00FF00"
                                                    (map_renderer/render map-id layer-id
                                            
                                                                )) layer-ids)]))
                     (map (fn [[map-id layer-surfaces]]
                            (let [w (img-width (first layer-surfaces))
                                  h (img-height (first layer-surfaces))]
                              [map-id (reduce (fn [map-surface layer-surface]
                                                (draw-to-surface map-surface
                                                                 (Draw layer-surface [0 0])))
                                              (image w h)
                                              layer-surfaces)])))
                     (into {}))))
       ;; Side-effecting hack to make it easyish to update the gui
       (r/map (fn [r]
                (doseq [[_ func] @rendered-maps-watchers]
                  (func))
                r))))

(defn layer-metadata-of!
  "Returns layer-metadatas associated with the map-id"
  [map-id]
  (->> @local-registry

       (filter #(and
                  (= (-> % second :type) :layer)
                  (= (-> % second :subtype) :layer)
                  (= (-> % second :parent-id) map-id)))
       (mapv second)))
