(ns merpg.UI.tree
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer :all]
            [seesaw.dev :refer :all]
            [clojure.core :refer :all]
            [clojure.string :as str]))

(defn our-renderer [renderer {:keys [value]}]
  (try
    (let [text
          (if (contains? (meta value) :tyyppi)
            (str (:tyyppi (meta value)))
            ":typerää"
            ;; (if (> (count (str value)) 30)
            ;;     (subs (str value) 0 30)
            ;;     (str value)
            ;;                             )
            )]
      (config! renderer :text text))
    (catch Exception ex
      #break
      (>pprint value)
      (throw ex))))

(defn adsasd []
  (frame :width 400
         :height 300
         :visible? true
         :content
         (scrollable
          (tree
           :renderer our-renderer
           :model (simple-tree-model
                   (fn [value]
                     (some #(= (-> value meta :tyyppi) %) [:root :map :layer]))
                   identity
                   (-> merpg.core/root :maps deref))
           :size [200 :by 300]
           :preferred-size [200 :by 300]
           ))))
(merpg.core/-main)
(adsasd)
