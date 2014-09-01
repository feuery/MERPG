;; (ns merpg.vfs
;;   (:require [clojure.test :refer :all]
;;             [merpg.util :refer [eq-gensym]]))

;; (defn nodify [element & {:keys [directory?]
;;                          :or {directory? true}}]
;;   {:pre [(not (nil? (-> parent meta :node-type)))]}
;;   (vary-meta element assoc
;;              :node-type (if directory? :directory :file)
;;              :node-name ""
;;              :node-id (eq-gensym)))

;; (defn make-directory [& stuff]
;;   (nodify stuff :directory? true))

;; (defn is-directory [node]
;;   (-> node meta :node-type (= :directory)))

;; ;; (defn parent
;; ;;   ([node]
;; ;;      {:pre [(->> node meta keys (some #(or (= % :node-type)
;; ;;                                            (= % :parent))))]}
;; ;;      (-> node meta :parent))
;; ;;   ([node parent]
;; ;;      {:pre [(->> node meta keys (some #(or (= % :node-type)
;; ;;                                            (= % :parent))))]}
;; ;;      (vary-meta node assoc :parent parent)))

;; (defn node-name
;;   ([node]
;;      {:pre [(do
;;               (println "meta-node keys: " (-> node meta keys))
;;               (->> node meta keys (some #(= % :node-type))))]}
;;      (-> node meta :node-name))
;;   ([node name]
;;      {:pre [(do
;;               (println "meta-node keys: " (-> node meta keys))
;;               (->> node meta keys (some #(= % :node-type))))]}
;;      (vary-meta node assoc :node-name name)))

;; (defn node-id [node]
;;   (-> node meta :node-id))

;; ;; (deftest directory-tests
;; ;;   (is (-> @merpg.UI.main-layout/map-set-image nodify vec make-directory is-directory)))
