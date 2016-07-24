(ns merpg.mutable.scripts
  (:require [merpg.mutable.registry :as re]
            [merpg.UI.events :as e]
            [clojure.string :as st]))

(defn script! [ns parent-id name]
  {:pre [(symbol? ns)
         (keyword? parent-id)
         (string? name)]}
  (let [id (keyword (gensym "SCRIPT__"))]
    (e/allow-events
     (re/register-element! id
                           {:name name
                            :parent-id parent-id
                            :order (count (re/query! #(and (= (:type %) :script)
                                                           (= (:parent-id %) parent-id))))
                            :ns ns
                            :src (str "(ns " (str ns) ")\n\n(println \"Hello world!\")")
                            :type :script
                            :id id
                            :onstartup false
                            :onexit false }))))
