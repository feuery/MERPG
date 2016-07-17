(ns merpg.mutable.drawing-queue
  (:require [merpg.2D.core :refer :all]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.registry :as re]
            [reagi.core :as r]))

(defn- query [fn r]
  (filter #(fn (second %)) r))

(def drawing-queues-per-map (->> (r/sample 200 re/registry)
                                 (r/map (fn [r]
                                          (let [map-ids (->> r
                                                             (query #(= (:type %) :map))
                                                             keys)]
                                            (zipmap map-ids (->> map-ids
                                                                 (mapv
                                                                  (fn [mapid]
                                                                    (->> r
                                                                         (query #(and (= (:type %) :sprite)
                                                                                      (= (:parent-id %) mapid)))
                                                                         vals
                                                                         (sort-by :order)
                                                                         vec))))))))))


