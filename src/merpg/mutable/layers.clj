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
  [W H & {:keys [hit? parent-id order] :or {hit? false
                                            parent-id nil
                                            order -1}}]
  (let [id (keyword (gensym "LAYER__"))]
    (doseq [[map-x map-y]
            (for [x (range W)
                  y (range H)]
              [x y])]
      (if hit?
        (t/hit-tile! false map-x map-y id)
        (t/tile! 0 0 :initial 0 map-x map-y id)))

    (re/register-element! id {:name "New layer"
                             :opacity 255
                             :visible? true
                             :type :layer
                             :parent-id parent-id
                             :order order
                             :subtype (if hit?
                                        :hitlayer
                                        :layer)})))

(tt/make-atom-binding layer-metas {:allow-seq? true}
                      (->> rv/local-registry
                           (r/map (fn [r]
                                    (->> r
                                         (filterv #(and
                                                   (= (-> % second :parent-id) (re/peek-registry :selected-map))
                                                   (= (-> % second :type) :layer)
                                                   (= (-> % second :subtype) :layer))))))))

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
