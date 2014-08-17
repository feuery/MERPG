(ns merpg.util
  (:require [clojure.string :refer [join]]))

(defmacro with-meta-of [from to]
  `(with-meta ~to (meta ~from)))

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

(defn eq-gensym
  "Generates not useless-symbols"
  []
  (->> (repeatedly 5 #(rand-int 100))
       (map str)
       join
       (str "S")
       keyword))
