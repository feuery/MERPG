(ns merpg.mutable.to-registry-binding
  "This ns provides a way to bind atoms back to the registry"
  (:require [merpg.mutable.registry :as re]
            [clojure.core.async :as a]))

(defn atom->registry-binding
  "Creates atoms with an initial value of whatever it is on the registry on the key. Changes to atoms are propagated to the registry BUT NOT VICE VERSA. This makes working with (gui-)apis that provide incomplete data as an an easy.

When the atom is changed, :meta - key with its value is stripped out. This eases working with the askbox"
  [key]
  (let [a (atom (re/peek-registry key))]
    (add-watch a :a->r-binding #(if-some [data %4]
                                  (do
                                    (re/register-element! key (if (associative? data)
                                                                (dissoc data :meta)
                                                                data))

                                    ;; This is a fugly hack
                                    ;; re/register-element! blocked the whole process when part of any suitable reagi stream
                                    (when (= key :selected-map)
                                      (a/go
                                        (Thread/sleep 100)
                                        (let [selected-layer  (->> @merpg.mutable.layers/layer-metas-ui
                                                                   first first)]
                                          (re/register-element! :selected-layer selected-layer))))
                                    (println key " is now " data))
                                  (println "Got nil-data in atom->registry-binding [" key "]")))
    a))
