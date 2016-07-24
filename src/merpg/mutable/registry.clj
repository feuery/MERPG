(ns merpg.mutable.registry
  (:require [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [postwalk]]
            [reagi.core :as r]

            [merpg.mutable.registry-views :as rv]
            [merpg.macros.multi :refer :all]
            [merpg.util :refer [in?]]))

(def ^:dynamic registry (atom {}))
(def render-allowed? (atom false))
(def watches (atom {}))

(defn is-render-allowed? []
  @render-allowed?) ;; I can't be arsed to trick the java type checked to use atoms correctly

;; implement a way to temporarily cease propagating registry to the reagi streams?
;; it might be necessary if they lock the app when heavily reorganizing registry
;; ie. onload

(add-watch registry :layer-view-updater #(reset! rv/local-registry-atom %4))

(defn query [fun registry]
  (->> registry
       (filter #(fun (-> % second)))))

(defn query! [fun]
  (query fun @registry))

(defn- run-watches! [id new-val]
  (let [new-atom (atom new-val)]
    (if-let [fns (get @watches id)]
      (doseq [[_ f] fns]
        (try
          (f new-atom)
          (catch Exception ex
            (pprint ex)))))
    (if (and (map? @new-atom)
             (contains? @new-atom :type))
      (when-let [type-fns (get (:types @watches) (:type @new-atom))]
        (doseq [[_ f] type-fns]
          (try 
            (f new-atom)
            (catch Exception ex
              (pprint ex))))))
    @new-atom))

(defn update-element!
  ([id fn]
   (swap! registry update id (comp (partial run-watches! id ) fn))))

(defn remove-element! [id]
  (swap! registry dissoc id)
  (let [removeable-ids (->> @registry
                            (filter #(-> % second :parent-id (= id)))
                            (map first))]
    (doseq [id removeable-ids]
      (remove-element! id))))

(defn register-element!
  "Returns id. Except for the [id element & rest] arity"
  ([id element]
   {:pre [(or (not= id :selected-tool)
              (not (coll? element)))]}
   ;; element has to know its own id due to how popupmenu in the domtree works
   (let [element (->> (if (coll? element)
                        (assoc element :id id)
                        element)
                      (run-watches! id))]
     (swap! registry assoc id element))
   id)
  ([element]
   (let [id (keyword (gensym))]
     (register-element! id element)))
  ([id element & rest]
   {:pre [(even? (count rest))]}

   (register-element! id element)
   (doseq [[key element] (partition 2 rest)]
     (register-element! key element))))

(defn peek-registry [id]
  (get @registry id))

(defmacro update-registry
  "Swap!s stuff in registry with a function built from the \"forms\" parameter. 
  Within forms you can refer to the registry's element with it's id like this:

(register-element! :age-of-another-obj 3)
(update-registry :age-of-obj
                 (println (str \"Age of object is \" :age-of-object))
                 (println \"Increasing...\")
                 (inc :age-of-obj))

  Currently returns the whole registry. Do not count on the return value though."
  
  [id & forms]
  (let [element-sym (gensym)
        forms (postwalk (fn [e]
                          (if (= e id)
                           element-sym
                           e))
                        forms)]
    `(update-element! ~id
                      (fn [~element-sym]
                        ~@forms))))

(defn children-of [registry id & {:keys [exclude-types] :or {exclude-types []}}]
  (query #(and (= (:parent-id %) id)
               (not (in? exclude-types (:type %)))) registry))

(defn children-of! [id & {:keys [exclude-types] :or {exclude-types []}}]
  (children-of @registry id :exclude-types exclude-types))
       

;; watches

(defn set-watch! [id watch-key fun]
  (if-not (contains? @watches id)
    (swap! watches assoc id {}))

  (swap! watches update id assoc watch-key fun))

(defn drop-watch! [id watch-key]
  (swap! watches update id dissoc watch-key))

(defn set-type-watch! [type-id watch-key fun]
  (if-not (contains? (:types @watches) type-id)
    (swap! watches assoc-in [:types type-id] {watch-key fun})
    (swap! watches update-in [:types type-id] assoc watch-key fun)))

(defn drop-type-watch! [type-id watch-key]
  (swap! watches update-in [:types type-id] dissoc watch-key))
