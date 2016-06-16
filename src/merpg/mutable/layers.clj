(ns merpg.mutable.layers
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [reagi.core :as r]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.tiles :as t]
            [merpg.mutable.tools :as tt]))

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

(def layers-view-per-maps (->> rv/local-registry
                               (r/map (fn [r]
                                        (->> r
                                             (filter #(and
                                                       (= (-> % second :type) :layer)
                                                       (= (-> % second :subtype) :layer)))
                                             (partition-by #(-> % second :parent-id))
                                             (map #(do ;; [:map-id :layer-ids]
                                                     [(-> % first second :parent-id)  %]))
                                             (into {})
                                             (mapvals (fn [layers]
                                                        (sort-by #(-> % second :order) layers)))
                                             
                                             (mapvals #(->> %
                                                            (pmap (comp
                                                                   (fn [layer-id]
                                                                     (rv/registry-to-layer @rv/local-registry layer-id))
                                                                   ;; registry-to-layer builds up the data in a way that you can refer to layer 0's tile at [1 2] with the form (get-in @layers-view [0 1 2])
                                                                   ;; thus we can't simply (map second), that loads only the metadata
                                                                   first))
                                                            vec)))))))

(def layers-view (->> layers-view-per-maps
                      (r/map (fn [r]
                               (get r (re/peek-registry :selected-map))))))

(defn layer-count! [map-id]
  (count (first (get @layers-view-per-maps map-id))))

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
                                (r/map #(rv/registry-to-layer @rv/local-registry %))))

(defn renderable-layers-of!
  "Returns layers associated with the map-id in a renderable form (with tiles)"
  [map-id]

  (->> @layers-view
       (filterv #(= map-id
                      (-> %
                          (get-in [0 0])
                          :map-id)))))
