(ns merpg.mutable.registry-views-test
  (:require [clojure.pprint :refer :all]
            [clojure.test :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :refer [layer-ids ] :as rv]
            [merpg.mutable.layers :refer [registry-to-layer
                                          layers-view]]
            [merpg.macros.test-registry :refer :all]
            [merpg.mutable.maps :as m]))

(deftest testing-layer-partitioning
  (clear-registry
   (let [map-id (m/map! 4 4 2)
         registry @re/registry
         layer-ids (layer-ids registry map-id)]
     (let [first-layer (first (map registry-to-layer layer-ids))]
       
       (dotimes [x 4]
         (dotimes [y 4]
           (let [{:keys [map-x map-y]} (get-in first-layer [x y])]
             (is [x y] [map-x map-y]))))))))

(deftest testing-layers-view
  (clear-registry
   (let [map-id (m/map! 4 4 2)
         anothermap-id (m/map! 5 5 2)]
     (println "")
     (Thread/sleep 1000)
     (let [layers @layers-view
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
                          :map-y)])))))))
