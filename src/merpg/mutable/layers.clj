(ns merpg.mutable.layers
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [reagi.core :as r]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.tiles :as t]
            [merpg.mutable.tools :as tt]
            [merpg.UI.askbox :refer [in?]]))

(println "Loading merpg.mutable.layers")

(defn layer!
  "Creates layer's tiles. Returns the id layer is registered with."
  [W H & {:keys [hit? parent-id order name] :or {hit? false
                                            parent-id nil
                                                 order -1
                                                 name "New layer"}}]
  (let [id (keyword (gensym "LAYER__"))]
    (doseq [[map-x map-y]
            (for [x (range W)
                  y (range H)]
              [x y])]
      (if hit?
        (t/hit-tile! false map-x map-y id)
        (t/tile! 0 0 :initial 0 map-x map-y id)))

    (re/register-element! id {:name name
                             :opacity 255
                             :visible? true
                             :type :layer
                             :parent-id parent-id
                             :order order
                             :subtype (if hit?
                                        :hitlayer
                                        :layer)})))

(defn mapvals
  "Maps function over hashmap's values"
  [f m]
  (into {}
        (for [[k v] m]
          [k (f v)])))

(tt/make-atom-binding layer-metas {:allow-seq? true}
                      (->> rv/local-registry
                           (r/map (fn [r]
                                    (->> r
                                         (filterv #(and
                                                    (= (-> % second :parent-id) (re/peek-registry :selected-map))
                                                    (= (-> % second :type) :layer)
                                                    (= (-> % second :subtype) :layer)))
                                         (sort-by #(-> % second :order))
                                         reverse)))))

(deftest layer-testing
  (binding [re/registry (atom {})]
    (let [layer-id (layer! 4 4)
          {name :name
           opacity :opacity
           visible? :visible?} (re/peek-registry layer-id)]
      (are [x y] (= x y)
        (count @re/registry) 17
        name "New layer"
        opacity 255
        visible? true)
      
      (re/update-registry layer-id
                          ;; I'll create CES/core.async based registry views soon...ish
                          (let [tiles (->> @re/registry
                                           (filter #(and (= (:parent-id (second %)) layer-id)
                                                         (= (:type (second %)) :tile)))
                                           (map first))]
                            ;; Increase rotation to 1
                            (doseq [tile-id tiles]
                              (re/update-registry tile-id
                                                  (update tile-id :rotation inc)))
                            ;; Make sure the updates are in the registry
                            (let [real-tiles (->> tiles
                                                  (map re/peek-registry ))]
                              (doseq [tile real-tiles]
                                (is (:rotation tile) 1))))))))

(defn sort-by-multiple-keys [col & keys]
  (sort-by #(vec (map % keys)) col))

;; these can't depend on indexable-layers-view which depends on registry-to-layer which depends on these
(defn mapwidth! [map-id]
  (let [layer-ids (->> @rv/local-registry
                       (filter #(and (= (-> % second :subtype) :layer)
                                     (= (-> % second :parent-id) map-id)))
                       (map first))
        tiles (->> @rv/local-registry
                   (filter #(and (= (-> % second :type) :tile)
                                 (in? layer-ids (-> % second :parent-id))))
                   (map second))
        xs (->> tiles
                (map :map-x)
                (into #{}))]
    (inc (apply max xs))))

(defn mapheight! [map-id]
  (let [layer-ids (->> @rv/local-registry
                       (filter #(and (= (-> % second :type) :layer)
                                     (= (-> % second :parent-id) map-id)))
                       (map first))
        tiles (->> @rv/local-registry
                   try
                   (filter #(and (= (-> % second :type) :tile)
                                 (in? layer-ids (-> % second :parent-id))))
                   (map (comp :map-y second))
                   (into #{}))]
    (inc (apply max tiles))))

(defn registry-to-layer
  [registry layer-id]
  (let [mapid (-> registry
                  (get layer-id)
                  :parent-id)
        meta (get @rv/local-registry layer-id)
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
          w (mapwidth! mapid)
          h (mapheight! mapid)]
      ;; (println "@registry-to-layer")
      ;; (println [w h])
      (if (zero? w) (println "w is zero on " mapid))
      
      (if (and (pos? w)
               (pos? h))
        (->> tiles
             (partition w)
             (mapv vec))))))

(def indexable-layers-view (->> rv/local-registry
                                (r/map (fn [r]
                                         (->> r
                                              (filter #(and
                                                        (= (-> % second :type) :layer)
                                                        (= (-> % second :subtype) :layer)))
                                              ;; sort them by their map-ids so that (into {}) won't barf
                                              (sort-by #(-> % second :parent-id))
                                              ;; partition them by their map-ids to make (into {}) possible
                                              (partition-by #(-> % second :parent-id))
                                              (mapv #(do ;;[map-id :layer-ids]
                                                       [(-> % first second :parent-id)  %]))
                                              (into {})
                                              (mapvals (partial into {}))
                                              (mapvals (fn [layer-map]
                                                         (->> layer-map
                                                              (mapv (fn [[layer-id _]]
                                                                      [layer-id (registry-to-layer @rv/local-registry layer-id)]))
                                                              (into {})))))))))

(defn get-renderable-layer! [mapid layerid]
  (if (realized? indexable-layers-view)
    (get-in @indexable-layers-view [mapid layerid])
    nil))


(def layers-view (->> indexable-layers-view
                      (r/map (fn [r]
                               (get-in r [(re/peek-registry :selected-map)
                                          (re/peek-registry :selected-layer)])))))

(defn layer-count! [map-id]
  (count (get @indexable-layers-view map-id)))

(def current-hitlayer (->> rv/local-registry
                           (r/map (fn [r]
                                    (->> r
                                         (filter #(and
                                                   (= (-> % second :type) :layer)
                                                   (= (-> % second :subtype) :hitlayer)
                                                   (= (-> % second :parent-id) (re/peek-registry :selected-map))))
                                         first)))))

(def current-hitlayer-data (->> current-hitlayer
                                (r/filter some?)
                                (r/map first)
                                (r/map #(registry-to-layer @rv/local-registry %))))         
       

(defn renderable-layers-of!
  "Returns layers associated with the map-id in a renderable form (with tiles)"
  [map-id]

  (->> @layers-view
       (filterv #(= map-id
                      (-> %
                          (get-in [0 0])
                          :map-id)))))

(println "Loaded merpg.mutable.layers")
