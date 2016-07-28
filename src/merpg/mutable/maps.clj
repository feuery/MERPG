(ns merpg.mutable.maps
  (:require [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.layers :as l]
            [merpg.mutable.tools :as tt]
            [merpg.mutable.tiles :as ti]
            [merpg.events.mouse :as m]
            [merpg.macros.multi :refer [def-real-multi]]
            [merpg.reagi :refer [editor-stream]]
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
                             :initial-map id
                             :selected-tool :pen
                             :selected-tile (ti/tile 0 0 :initial 0))))
    id))

(defn- get-import-list [orig-forms kw-type]
  (loop [forms (drop 2 orig-forms)
         depth 0]
    (if (= (-> forms first first) kw-type)
      (if (= kw-type :require)
        (conj (rest (first forms)) '[clojure.core :refer :all])
        (rest (first forms)))
      (if (empty? forms)
        (if (= kw-type :require)
          '([clojure.core :refer :all])
          '())
        (if (> depth 100)
          (throw (Exception. (str "Depth of 100 calls reached in get-import-list for parameters" [orig-forms kw-type])))
          (recur (rest forms) (inc depth)))))))
  
(defn parse-ns [textual-src]
  {:pre [(-> textual-src read-string first (= 'ns))]}
  (let [ns-form (read-string textual-src)
        ns (second ns-form)
        r-list (get-import-list ns-form :require)
        i-list (get-import-list ns-form :import)]
    (let [require-list (->> r-list
                            (map (fn [ns]
                                   (list require (list 'quote ns))))
                            (into '[do])
                            (apply list))
          import-list (->> i-list
                           (map (fn [ns]
                                  (list 'import (list 'quote ns))))
                           (into '[do])
                           (apply list))]
      {:script-ns ns
       :requires require-list
       :imports import-list})))
    
                             

(re/set-watch! :selected-map :script-loader (fn [selected-map]
                                              (let [selected-map @selected-map
                                                    scripts (re/query! #(and (= (:type %) :script)
                                                                             (= (:parent-id %) selected-map)))]
                                                (doseq [[_ script] scripts]
                                                  (let [{:keys [script-ns
                                                                requires
                                                                imports]} (parse-ns (:src script))
                                                        ns-form (-> script :src read-string)
                                                        ast (->> (str "(do " (:src script) ")")
                                                                 read-string
                                                                 (filter #(not= % ns-form)))]
                                                    (try
                                                      (binding [*ns* (or (find-ns script-ns)
                                                                         (create-ns script-ns))]
                                                        (if (some? *ns*)
                                                          (do
                                                            (eval requires)
                                                            (eval imports)
                                                            (eval ast))
                                                          (println "*ns* is nil")))
                                                      (catch Exception ex
                                                        (pprint ex))))))))

(tt/make-atom-binding map-metas {:allow-seq? true}
                      (editor-stream (r/sample 700 re/registry)
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
        tile-id))))

(def map-events (editor-stream m/mouse-events
                     (r/filter #(= (:source %) :map-controller))
                     (r/map (fn [{:keys [tile-x tile-y type]}]
                              (let [tile-id (get-edited-tile! tile-x tile-y)
                                    tool-id (re/peek-registry :selected-tool)
                                    {tool-fn :fn} (re/peek-registry tool-id)]
                                (if (fn? tool-fn)
                                  (do
                                    (tool-fn tile-id)
                                    {:success? true
                                     :coords [tile-x tile-y]})
                                  (if (and (map? tool-fn)
                                           (contains? tool-fn type))
                                    (let [tool-fn (get tool-fn type)]
                                      (tool-fn tile-id)
                                      {:success? true
                                       :coords [tile-x tile-y]})
                                    
                                    {:success? false
                                     :coords [tile-x tile-y]})))))))
