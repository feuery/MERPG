(ns merpg.mutable.registry-views-test
  (:require [clojure.pprint :refer :all]
            [clojure.test :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :refer [layer-ids registry-to-layer
                                                  layers-view] :as rv]
            [merpg.macros.test-registry :refer :all]
            [merpg.mutable.maps :as m]))

(deftest testing-layer-partitioning
  (clear-registry
    (let [map-id (m/map! 4 4 2)
          registry @re/registry
          layer-ids (layer-ids registry map-id)
          first-layer (first (map (partial registry-to-layer registry) layer-ids))]
      
      (dotimes [x 4]
        (dotimes [y 4]
          (let [{:keys [map-x map-y]} (get-in first-layer [x y])]
            (is [x y] [map-x map-y])))))))

(deftest testing-layers-view
  (clear-registry
    (let [map-id (m/map! 4 4 2)
          anothermap-id (m/map! 5 5 2)
          layers @layers-view
          w (-> layers
                flatten
                (rv/highest-key :map-x)
                inc)
          h (-> layers
                flatten
                (rv/highest-key :map-y)
                inc)]
      (dotimes [x w]
        (dotimes [y h]
          (is [x y] [(-> layers
                         (get-in [x y])
                         :map-x)
                     (-> layers
                         (get-in [x y])
                         :map-y)]))))))

(deftest renderable-layers-of-test
  (clear-registry
    (let [map1 (m/map! 5 5 6)
          map2 (m/map! 2 2 3)]
      (Thread/sleep 10)
      (are [x y] (= x y)
        (count (rv/renderable-layers-of! map1)) 6
        (count (rv/renderable-layers-of! map2)) 3))))
