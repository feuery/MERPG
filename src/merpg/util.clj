(ns merpg.util
  (:require [merpg.immutable.basic-map-stuff :refer [with-meta-of]]))

(defn vec-remove ;;dissoc is bloody useless
  "remove elem in coll"
  [coll pos]
  (with-meta-of coll
    (vec (concat (subvec coll 0 pos) (subvec coll (inc pos))))))

(def abs #(java.lang.Math/abs %))

(defn cos [angle]
  (Math/cos (Math/toRadians angle)))

(defn sin [angle]
  (Math/sin (Math/toRadians angle)))

(defn vec-insert [vector index element]
  (let [[first rest] [(subvec vector 0 (inc index))
                      (subvec vector (inc index))]]
    (apply conj first element rest)))
