(ns merpg.mutable.to-registry-binding
  "This ns provides a way to bind atoms back to the registry"
  (:require [merpg.mutable.registry :as re]))

(defn atom->registry-binding [key]
  (let [a (atom (re/peek-registry key))]
    (add-watch a :a->r-binding #(if-some [data %4]
                                  (re/register-element! key data)
                                  (println "Got nil-data in atom->registry-binding [" key "]")))
    a))
