(ns merpg.mutable.resize-algorithms
  (:require [merpg.macros.multi :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.tiles :refer [hit-tile! tile!]]
            [merpg.mutable.layers :as l :refer [mapwidth! mapheight!]]

            [clojure.pprint :refer :all]))

(defn resize! [map-id new-w new-h]
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
                ;; (println "loop coords: " [x-ind y-ind])
                (let [tile (get-in layer-tiles [layer-id x-ind y-ind])]
                  (if-some [{:keys [tile-id]} (get-in @l/indexable-layers-view [map-id new-layer-id x-ind y-ind])]
                    (do
                      ;; (println "tile-id to edit: " tile-id)
                      ;; (println "tile coords: " [(:x tile) (:y tile)])
                      (re/update-registry tile-id
                                          (assoc tile-id
                                                 :x (:x tile)
                                                 :y (:y tile)
                                                 :tileset (:tileset tile)
                                                 :rotation (:rotation tile))))
                    (println "Tile-id at " [map-id new-layer-id x-ind y-ind] " is nil")))))))))))
            ;;     (let [
            ;;           {:keys [x y tileset rotation] :as tile} (get-in layer-tiles [layer-id x-ind y-ind])]
            ;;       (println tileset)
;; )))         
            ;; ))))))

        

;; (def-real-multi resize! [map-id new-w new-h horizontal-anchor vertical-anchor]
;;   (do
;;     (pprint [map-id new-w new-h horizontal-anchor vertical-anchor])
;;     {:hanchor horizontal-anchor
;;      :vanchor vertical-anchor
;;      :add-x (pos? (- new-w (mapwidth! map-id)))
;;      :add-y (pos? (- new-h (mapheight! map-id)))}))
              
;; (defmethod resize!
;;   {:hanchor :right
;;    :vanchor :bottom
;;    :add-x true
;;    :add-y true}
;;   [map-id new-w new-h horizontal-anchor vertical-anchor]
;;   (let [w (mapwidth! map-id)
;;         h (mapheight! map-id)
;;         layer-ids (->> @re/registry
;;                        (filter #(and
;;                                  (= (-> % second :subtype) :layer)
;;                                  (= (-> % second :parent-id) map-id)))
;;                        (map first))
;;         hit-layer-id (->> @re/registry
;;                           (filter #(and
;;                                     (= (-> % second :subtype) :hitlayer)
;;                                     (= (-> % second :parent-id) map-id)))
;;                           first
;;                           first)
        
        
;;         coords (for [x (range w new-w)
;;                      y (range h)]
;;                  [x y])]
;;     (pprint coords)
;;     (doseq [l layer-ids]
;;       (doseq [[x y] coords]
;;         (tile! 0 0 :initial 0 x y l :debug? true)))
;;     (doseq [[x y] coords]
;;       (hit-tile! false x y hit-layer-id))))
;; ;
