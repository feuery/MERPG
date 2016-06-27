(ns merpg.mutable.layers-test
  (:require [clojure.pprint :refer :all]
            [clojure.test :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :refer [layer-ids ] :as rv]
            [merpg.mutable.layers :refer [layer!]]
            [merpg.macros.test-registry :refer :all]
            [merpg.mutable.maps :as m]))

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
