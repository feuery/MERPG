(ns merpg.mutable.resize-algorithms
  (:require [merpg.macros.multi :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.tiles :refer [hit-tile! tile!]]
            [merpg.mutable.layers :as l :refer [mapwidth! mapheight!]]

            [clojure.pprint :refer :all]))

(defn resize! [map-id new-w new-h]
  (throw (Exception. "Not implemented")))

(defn resize!-bottom-right [map-id new-w new-h]
  (locking *out*
    (let [layer-ids (->> @re/registry
                         (filter #(and (= (-> % second :type) :layer)
                                       (= (-> % second :parent-id) map-id)))
                         (map first))
          layer-tiles (->> layer-ids
                           (mapv (fn [layer-id]
                                   [layer-id (get-in @l/indexable-layers-view [map-id layer-id])]))
                           (into {}))]
      (doseq [layer-id layer-ids]
        (let [{:keys [name order subtype] :as layer} (re/peek-registry layer-id)]
          (re/remove-element! layer-id)
          (let [new-layer-id (l/layer! new-w new-h
                                       :parent-id map-id
                                       :hit? (= subtype :hitlayer)
                                       :order order
                                       :name name)]
            (Thread/sleep 500)
            (dotimes [x-ind (count (get layer-tiles layer-id))]
              (dotimes [y-ind (-> (get-in layer-tiles [layer-id x-ind])
                                  count)]
                (let [tile (get-in layer-tiles [layer-id x-ind y-ind])]
                  (if-some [{:keys [tile-id]} (get-in @l/indexable-layers-view [map-id new-layer-id x-ind y-ind])]
                    (do
                      (re/update-registry tile-id
                                          (assoc tile-id
                                                 :x (:x tile)
                                                 :y (:y tile)
                                                 :tileset (:tileset tile)
                                                 :rotation (:rotation tile))))
                    (println "Tile-id at " [map-id new-layer-id x-ind y-ind] " is nil")))))))))))

(defn resize!-top-left [map-id new-w new-h]
  (locking *out*
    (let [layer-ids (->> @re/registry
                         (filter #(and (= (-> % second :type) :layer)
                                       (= (-> % second :parent-id) map-id)))
                         (map first))
          layer-tiles (->> layer-ids
                           (mapv (fn [layer-id]
                                   [layer-id (get-in @l/indexable-layers-view [map-id layer-id])]))
                           (into {}))
          h-diff (- new-h (mapheight! map-id))
          w-diff (- new-w (mapwidth! map-id))]
      (doseq [layer-id layer-ids]
        (let [{:keys [name order subtype] :as layer} (re/peek-registry layer-id)]
          (re/remove-element! layer-id)
          (let [new-layer-id (l/layer! new-w new-h
                                       :parent-id map-id
                                       :hit? (= subtype :hitlayer)
                                       :order order
                                       :name name)]
            (Thread/sleep 500)
            (dotimes [x-ind (count (get layer-tiles layer-id))]
              (dotimes [y-ind (-> (get-in layer-tiles [layer-id x-ind])
                                  count)]
                (let [tile (get-in layer-tiles [layer-id x-ind y-ind])]
                  (if-some [{:keys [tile-id]} (get-in @l/indexable-layers-view [map-id new-layer-id (+ w-diff x-ind) (+ y-ind h-diff)])]
                    (do
                      (re/update-registry tile-id
                                          (assoc tile-id
                                                 :x (:x tile)
                                                 :y (:y tile)
                                                 :tileset (:tileset tile)
                                                 :rotation (:rotation tile))))
                    (println "Tile-id at " [map-id new-layer-id x-ind y-ind] " is nil")))))))))))

(defn resize!-top-right [map-id new-w new-h]
  (locking *out*
    (let [layer-ids (->> @re/registry
                         (filter #(and (= (-> % second :type) :layer)
                                       (= (-> % second :parent-id) map-id)))
                         (map first))
          layer-tiles (->> layer-ids
                           (mapv (fn [layer-id]
                                   [layer-id (get-in @l/indexable-layers-view [map-id layer-id])]))
                           (into {}))
          h-diff (- new-h (mapheight! map-id))]
      (doseq [layer-id layer-ids]
        (let [{:keys [name order subtype] :as layer} (re/peek-registry layer-id)]
          (re/remove-element! layer-id)
          (let [new-layer-id (l/layer! new-w new-h
                                       :parent-id map-id
                                       :hit? (= subtype :hitlayer)
                                       :order order
                                       :name name)]
            (Thread/sleep 500)
            (dotimes [x-ind (count (get layer-tiles layer-id))]
              (dotimes [y-ind (-> (get-in layer-tiles [layer-id x-ind])
                                  count)]
                (let [tile (get-in layer-tiles [layer-id x-ind y-ind])]
                  (if-some [{:keys [tile-id]} (get-in @l/indexable-layers-view [map-id new-layer-id x-ind (+ y-ind h-diff)])]
                    (do
                      (re/update-registry tile-id
                                          (assoc tile-id
                                                 :x (:x tile)
                                                 :y (:y tile)
                                                 :tileset (:tileset tile)
                                                 :rotation (:rotation tile))))
                    (println "Tile-id at " [map-id new-layer-id x-ind y-ind] " is nil")))))))))))


(defn resize!-bottom-left [map-id new-w new-h]
  (locking *out*
    (let [layer-ids (->> @re/registry
                         (filter #(and (= (-> % second :type) :layer)
                                       (= (-> % second :parent-id) map-id)))
                         (map first))
          layer-tiles (->> layer-ids
                           (mapv (fn [layer-id]
                                   [layer-id (get-in @l/indexable-layers-view [map-id layer-id])]))
                           (into {}))
          x-diff (- new-w (mapwidth! map-id))]
      (doseq [layer-id layer-ids]
        (let [{:keys [name order subtype] :as layer} (re/peek-registry layer-id)]
          (re/remove-element! layer-id)
          (let [new-layer-id (l/layer! new-w new-h
                                       :parent-id map-id
                                       :hit? (= subtype :hitlayer)
                                       :order order
                                       :name name)]
            (Thread/sleep 500)
            (dotimes [x-ind (count (get layer-tiles layer-id))]
              (dotimes [y-ind (-> (get-in layer-tiles [layer-id x-ind])
                                  count)]
                (let [tile (get-in layer-tiles [layer-id x-ind y-ind])]
                  (if-some [{:keys [tile-id]} (get-in @l/indexable-layers-view [map-id new-layer-id (+ x-ind x-diff) y-ind])]
                    (do
                      (re/update-registry tile-id
                                          (assoc tile-id
                                                 :x (:x tile)
                                                 :y (:y tile)
                                                 :tileset (:tileset tile)
                                                 :rotation (:rotation tile))))
                    (println "Tile-id at " [map-id new-layer-id x-ind y-ind] " is nil")))))))))))

