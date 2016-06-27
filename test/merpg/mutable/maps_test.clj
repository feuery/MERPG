(ns merpg.mutable.maps-test
  (:require [clojure.pprint :refer :all]
            [clojure.test :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :refer [layer-ids ] :as rv]
            [merpg.mutable.layers :refer [registry-to-layer
                                          layers-view]]
            [merpg.macros.test-registry :refer :all]
            [merpg.mutable.maps :refer [map!]]))

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
