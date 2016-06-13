(ns merpg.mutable.to-registry-binding
  "This ns provides a way to bind atoms back to the registry"
  (:require [merpg.mutable.registry :as re]))

(defn atom->registry-binding [key]
  (let [a (atom (re/peek-registry key))]
    (add-watch a :a->r-binding #(re/register-element! key %4))
    a))
