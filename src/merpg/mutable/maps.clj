(ns merpg.mutable.maps
  (:require [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.layers :as l]
            [merpg.mutable.tools :as tt]
            [merpg.macros.multi :refer [def-real-multi]]
            [seesaw.core :as s]
            [reagi.core :as r]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn map!
  "Map-constructor. Not to be confused with the HOF map. Or Hashmaps.

  If no objects with :type :map exist in registry when calling this, this creates also :selected-layer, :selected-map and :selected-tool.

  Returns the id map is registered with"
  [W H layer-count]
  (let [first? (not (some #(= (-> % second :type) :map) @re/registry))
        id (keyword (gensym "MAP__"))
        layers (->> layer-count
                    range
                    (map (fn [order]
                           (l/layer! W H :parent-id id :order order)))
                    doall)
        hit-layer (l/layer! W H :hit? true :parent-id id)] ;;we need hitlayer too
    (doseq [layer-id layers]
      (re/update-registry layer-id
                         (assoc layer-id :parent-id id)))

    (re/update-registry hit-layer
                       (assoc hit-layer :parent-id id))

    (re/register-element! id {:name (str id)
                             :zonetiles {[0 0] #(s/alert "TODO: design real zonetiles")}
                             :type :map})

    (when first?
      (re/register-element! :selected-layer (first layers)
                           :selected-map id
                           :selected-tool :pen))
    id))

(map! 1 1 1) ;;initial 

(deftest map-testing
  (binding [re/registry (atom {})]
    (let [layer-count 3
          map-id (map! 10 10 layer-count)]
      (is (count @re/registry) 409)
      (let [layers (->> @re/registry
                    (filter #(and (= (:type (second %)) :layer)
                                  (= (:parent-id (second %)) map-id)))
                    (map second))
            hit-data (filter #(= (:subtype %) :hitlayer) layers)
            hit-layer (first hit-data)]
        (is (not (nil? hit-layer)))
        (is (count hit-data) 1)
        (is (count layers) layer-count)))))

(tt/make-atom-binding map-metas {:allow-seq? true}
                      (->> rv/local-registry
                           (r/map (fn [r]
                                    (->> r
                                         (filterv #(= (-> % second :type) :map))
                                         (into {}))))))
