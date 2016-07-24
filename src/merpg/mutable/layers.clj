(ns merpg.mutable.layers
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer :all]
            [merpg.reagi :refer [editor-stream]]
            [reagi.core :as r]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.tiles :as t]
            [merpg.mutable.tools :as tt]
            [merpg.util :refer [in? mapvals]]
            [merpg.UI.events :as e]))

(println "Loading merpg.mutable.layers")

(defn layer-meta [name parent-id order hit?]
  {:name name
   :opacity 255
   :visible? true
   :type :layer
   :parent-id parent-id
   :order order
   :subtype (if hit?
              :hitlayer
              :layer)})
(defn layer!
  "Creates layer's tiles. Returns the id layer is registered with."
  [W H & {:keys [hit? parent-id order name] :or {hit? false
                                                 parent-id nil
                                                 order -1
                                                 name "New layer"}}]
  (let [id (keyword (gensym "LAYER__"))]
    (doseq [[map-x map-y]
            (for [x (range W)
                  y (range H)]
              [x y])]
      (if hit?
        (t/hit-tile! false map-x map-y id)
        (t/tile! 0 0 :initial 0 map-x map-y id)))

    (e/allow-events
     (re/register-element! id (layer-meta (if hit?
                                            "Hitlayer"
                                            name)
                                          parent-id order hit?)))))

(tt/make-atom-binding layer-metas {:allow-seq? true}
                      (editor-stream (r/sample 1000 re/registry)
                           (r/map (fn [r]
                                    (->> r
                                         (filterv #(and
                                                    (= (-> % second :parent-id) (re/peek-registry :selected-map))
                                                    (= (-> % second :type) :layer)
                                                    (= (-> % second :subtype) :layer)))
                                         (sort-by #(-> % second :order))
                                         reverse)))))

(defn sort-by-multiple-keys [col & keys]
  (sort-by #(vec (map % keys)) col))

;; these can't depend on indexable-layers-view which depends on registry-to-layer which depends on these
(defn mapwidth! [map-id]
  (let [layer-ids (->> @re/registry
                       (filter #(and (= (-> % second :subtype) :layer)
                                     (= (-> % second :parent-id) map-id)))
                       (map first))
        tiles (->> @re/registry
                   (filter #(and (= (-> % second :type) :tile)
                                 (in? layer-ids (-> % second :parent-id))))
                   (map second))
        xs (->> tiles
                (map :map-x)
                (into #{}))]
    (if (empty? xs)
      0
      (inc (apply max xs)))))

(defn mapheight! [map-id]
  (let [layer-ids (->> @re/registry
                       (filter #(and (= (-> % second :type) :layer)
                                     (= (-> % second :parent-id) map-id)))
                       (map first))
        tiles (->> @re/registry
                   (filter #(and (= (-> % second :type) :tile)
                                 (in? layer-ids (-> % second :parent-id))))
                   (map (comp :map-y second))
                   (into #{}))]
    (if-not (empty? tiles)
      (inc (apply max tiles))
      0)))

(defn registry-to-layer
  [layer-id]
  (let [registry @re/registry
        mapid (-> registry
                  (get layer-id)
                  :parent-id)
        meta (get registry layer-id)
        tiles (->> registry
                   ;; get relevant tiles
                   (filter #(and (= (-> % second :type) :tile)
                                 (= (-> % second :parent-id) layer-id)))
                   ;; assoc map-ids their own ids to tiles
                   (map #(update % 1 assoc
                                 :map-id mapid
                                 :tile-id (first %)
                                 :meta meta))
                   (map second))]
    (when (empty? tiles)
      ;; Exception so that the tests fail.
      ;; Was it just returning nil, tests would incorrectly succeed
      (throw (Exception. "tiles is empty@registry-to-layer")))
    
    (let [;; sort by map-x map-y for easy partitioning
          tiles (sort-by-multiple-keys tiles :map-x :map-y)
          w (mapwidth! mapid)
          h (mapheight! mapid)]
      ;; (->> tiles
      ;;      (map :map-x)
      ;;      pprint)
      ;; (locking *out*
      ;;   (println "@registry-to-layer")               
      ;;   (println [w h])
        (if (zero? w) (println "w is zero on " mapid))
        
        (if (and (pos? w)
                 (pos? h))
          ;; tiles sisältää roskadataa
          (let [toret (->> tiles
                           (partition w)
                           (mapv vec))]
            ;; (println "@registry-to-layer, real w is " (count toret) ", assumed w is " w)
            ;; (pprint toret)
            toret)))))

(def indexable-layers-view (editor-stream (r/sample 30 re/registry)
                                (r/map (fn [r]
                                         ;; (if (re/is-render-allowed?)
                                         (->> r
                                              (filter #(and
                                                        (= (-> % second :type) :layer)
                                                        (= (-> % second :subtype) :layer)))
                                              ;; sort them by their map-ids so that (into {}) won't barf
                                              (sort-by #(-> % second :parent-id))
                                              ;; partition them by their map-ids to make (into {}) possible
                                              (partition-by #(-> % second :parent-id))
                                              (mapv #(do ;;[map-id :layer-ids]
                                                       [(-> % first second :parent-id)  %]))
                                              (into {})
                                              (mapvals (partial into {}))
                                              (mapvals (fn [layer-map]
                                                         (->> layer-map
                                                              (mapv (fn [[layer-id _]]
                                                                      [layer-id (registry-to-layer layer-id)]))
                                                              (into {})))))
                                         ))))

(defn get-renderable-layer! [mapid layerid]
  (if (realized? indexable-layers-view)
    (let [toret (get-in @indexable-layers-view [mapid layerid])]
      ;; (println "@get-renderable-layer: W of " mapid " is " (mapwidth! mapid) ". Width of the datastructure is " (count toret))
      toret)
    nil))


(def layers-view (editor-stream indexable-layers-view
                      (r/map (fn [r]
                               (get-in r [(re/peek-registry :selected-map)
                                          (re/peek-registry :selected-layer)])))))

(defn layer-count! [map-id]
  (count (get @indexable-layers-view map-id)))

(def current-hitlayer (editor-stream (r/sample 50 re/registry)
                           (r/map (fn [r]
                                    (->> r
                                         (filter #(and
                                                   (= (-> % second :type) :layer)
                                                   (= (-> % second :subtype) :hitlayer)
                                                   (= (-> % second :parent-id) (re/peek-registry :selected-map))))
                                         first)))))

(def current-hitlayer-data (editor-stream current-hitlayer
                                (r/filter some?)
                                (r/map first)
                                (r/map #(registry-to-layer %))))         

(println "Loaded merpg.mutable.layers")
