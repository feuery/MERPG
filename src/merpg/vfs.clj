(ns merpg.vfs
  (:require [clojure.test :refer :all]))

(defn nodify [element & {:keys [parent directory?]
                         :or {parent :root
                              directory? true}}]
  {:pre [(or (= parent :root)
             (not (nil? (-> parent meta :node-type))))]}
  (vary-meta element assoc :parent parent
             :node-type (if directory? :directory :file)
             :node-name ""))

(defn make-directory [& stuff]
  (nodify (vec stuff) :directory? true))

(defn is-directory [node]
  (-> node meta :node-type (= :directory)))

(defn parent
  ([node]
     {:pre [(->> node meta keys (some #(or (= % :node-type)
                                           (= % :parent))))]}
     (-> node meta :parent))
  ([node parent]
     {:pre [(->> node meta keys (some #(or (= % :node-type)
                                           (= % :parent))))]}
     (vary-meta node assoc :parent parent)))

(defn node-name
  ([node]
     {:pre [(->> node meta keys (some #(or (= % :node-type)
                                           (= % :parent))))]}
     (-> node meta :node-name))
  ([node name]
     {:pre [(->> node meta keys (some #(or (= % :node-type)
                                           (= % :parent))))]}
     (vary-meta node assoc :node-name name)))

(deftest directory-tests
  (is (-> @merpg.UI.main-layout/map-set-image nodify vec make-directory is-directory)))
