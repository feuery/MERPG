(ns merpg.mutable.maps
  (:require [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.layers :as l]
            [merpg.mutable.tools :as tt]
            [merpg.mutable.tiles :as ti]
            [merpg.events.mouse :as m]
            [merpg.macros.multi :refer [def-real-multi]]
            [merpg.UI.events :as e]
            [seesaw.core :as s]
            [reagi.core :as r]
            [clojure.test :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn map!
  "Map-constructor. Not to be confused with the HOF map. Or Hashmaps.

  If no objects with :type :map exist in registry when calling this, this creates also :selected-layer, :selected-map and :selected-tool.

  Returns the id map is registered with"
  [W H layer-count & {:keys [name] :or {name "New map"}}]
  (let [first? (not (some #(= (-> % second :type) :map) @re/registry))
        id (keyword (gensym "MAP__"))
        layers (->> layer-count
                    range
                    (map (fn [order]
                           (l/layer! W H :parent-id id :order order :name (str "Layer " order))))
                    doall)
        hit-layer (l/layer! W H :hit? true :parent-id id)] ;;we need hitlayer too
    (doseq [layer-id layers]
      (re/update-registry layer-id
                         (assoc layer-id :parent-id id)))

    (re/update-registry hit-layer
                       (assoc hit-layer :parent-id id))

    (e/allow-events
     (re/register-element! id {:name name
                               :parent-id :root
                               :type :map})

     (when first?
       (re/register-element! :selected-layer (first layers)
                             :selected-map id
                             :selected-tool :pen
                             :selected-tile (ti/tile 0 0 :initial 0))))
    id))

(map! 1 1 1 :name "Initial map") 

(tt/make-atom-binding map-metas {:allow-seq? true}
                      (->> rv/local-registry
                           (r/map (fn [r]
                                    (->> r
                                         (filterv #(= (-> % second :type) :map))
                                         (into {}))))))

(defn get-edited-tile! [tile-x tile-y]
  (let [tool-id (re/peek-registry :selected-tool)]
    (if (= tool-id :hit-tool)
      (let [{:keys [tile-id] :as tile} (get-in @l/current-hitlayer-data [tile-x tile-y])]
        tile-id)
      (let [{:keys [tile-id] :as tile} (get-in @l/layers-view [ tile-x tile-y])]
        (pprint [tile-x tile-y])
        tile-id))))

(def map-events (->> m/mouse-events
                      (r/filter #(= (:source %) :map-controller))
                      (r/map (fn [{:keys [tile-x tile-y]}]
                               (let [tile-id (get-edited-tile! tile-x tile-y)
                                     tool-id (re/peek-registry :selected-tool)
                                     {tool-fn :fn} (re/peek-registry tool-id)]
                                 (if (fn? tool-fn)
                                   (do
                                     (tool-fn tile-id)
                                     {:success? true
                                      :coords [tile-x tile-y]})
                                   {:success? false
                                    :coords [tile-x tile-y]}))))))



