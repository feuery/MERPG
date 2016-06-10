(ns merpg.mutable.maps
  (:require [merpg.mutable.registry :as r]
            [merpg.mutable.layers :as l]
            [merpg.macros.multi :refer [def-real-multi]]
            [seesaw.core :as s]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn map!
  "Map-constructor. Not to be confused with the HOF map. Or Hashmaps.

  If no objects with :type :map exist in registry when calling this, this creates also :selected-layer, :selected-map and :selected-tool.

  Returns the id map is registered with"
  [W H layer-count]
  (let [first? (not (some #(= (-> % second :type) :map) @r/registry))
        id (keyword (gensym "MAP__"))
        layers (->> layer-count
                    range
                    (map (fn [order]
                           (l/layer! W H :parent-id id :order order)))
                    doall)
        hit-layer (l/layer! W H :hit? true :parent-id id)] ;;we need hitlayer too
    (doseq [layer-id layers]
      (r/update-registry layer-id
                         (assoc layer-id :parent-id id)))

    (r/update-registry hit-layer
                       (assoc hit-layer :parent-id id))

    (r/register-element! id {:name (str id)
                             :zonetiles {[0 0] #(s/alert "TODO: design real zonetiles")}
                             :type :map})

    (when first?
      (r/register-element! :selected-layer 0
                           :selected-map 0
                           :selected-tool :pen))
    id))

(deftest map-testing
  (binding [r/registry (atom {})]
    (let [layer-count 3
          map-id (map! 10 10 layer-count)]
      (is (count @r/registry) 409)
      (let [layers (->> @r/registry
                    (filter #(and (= (:type (second %)) :layer)
                                  (= (:parent-id (second %)) map-id)))
                    (map second))
            hit-data (filter #(= (:subtype %) :hitlayer) layers)
            hit-layer (first hit-data)]
        (is (not (nil? hit-layer)))
        (is (count hit-data) 1)
        (is (count layers) layer-count)))))
