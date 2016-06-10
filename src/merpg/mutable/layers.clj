(ns merpg.mutable.layers
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [merpg.mutable.registry :as r]
            [merpg.mutable.tiles :as t]))

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

    (r/register-element! id {:name "New layer"
                             :opacity 255
                             :visible? true
                             :type :layer
                             :parent-id parent-id
                             :order order
                             :subtype (if hit?
                                        :hitlayer
                                        :layer)})))

;; (pprint @r/registry)

;; (layer! 3 3)

(deftest layer-testing
  (binding [r/registry (atom {})]
    (let [layer-id (layer! 4 4)
          {name :name
           opacity :opacity
           visible? :visible?} (r/peek-registry layer-id)]
      (are [x y] (= x y)
        (count @r/registry) 17
        name "New layer"
        opacity 255
        visible? true)
      
      (r/update-registry layer-id
                         ;; I'll create CES/core.async based registry views soon...ish
                         (let [tiles (->> @r/registry
                                          (filter #(and (= (:parent-id (second %)) layer-id)
                                                        (= (:type (second %)) :tile)))
                                          (map first))]
                           ;; Increase rotation to 1
                           (doseq [tile-id tiles]
                             (r/update-registry tile-id
                                                (update tile-id :rotation inc)))
                           ;; Make sure the updates are in the registry
                           (let [real-tiles (->> tiles
                                                 (map r/peek-registry ))]
                             (doseq [tile real-tiles]
                               (is (:rotation tile) 1))))))))
