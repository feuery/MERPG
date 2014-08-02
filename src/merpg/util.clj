(ns merpg.util
  (:require [merpg.immutable.basic-map-stuff :refer [with-meta-of]]))

(defn vec-remove ;;dissoc is bloody useless
  "remove elem in coll"
  [coll pos]
  (with-meta-of coll
    (vec (concat (subvec coll 0 pos) (subvec coll (inc pos))))))
