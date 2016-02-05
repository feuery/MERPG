(ns merpg.UI.tree
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer :all]
            [seesaw.dev :refer :all]
            [clojure.core :refer :all]
            [clojure.string :as str]
            [merpg.core :refer :all])
  (:import [javax.swing.tree DefaultTreeModel DefaultMutableTreeNode]))

(defn our-renderer [renderer {:keys [value] :as wut}]
  (try
    (let [text
          (if (contains? (meta value) :tyyppi)
            (str (:tyyppi (meta value)))
            (str value))]
      (config! renderer :text text))
    (catch Exception ex
      (>pprint value)
      (throw ex))))

(defn create [title parent-node]
  (try
    (let [child (DefaultMutableTreeNode. title)]
      (when-not (nil? parent-node)
        (.insert parent-node child 0))
      child)
    (catch NullPointerException ex
      #break
      (println "wtf?"))))
    

(defn build-model! [model children parent]
  (doseq [child children]
    (if (contains? (meta child) :tyyppi)
      (let [node (create
                  child
                  parent
                  )]
        (if (sequential? child)
          (build-model! model child node))))))

(defn node-selected [e]
  ;; To get the metadata of the selected object use
  ;; (-> e seesaw.core/selection last .getUserObject meta)
  (alert (str (selection e))))


(-main)

(defn adsasd []
  (let [root-node (create ":root" nil)
        model (DefaultTreeModel. root-node)]
    (build-model! model @(:maps merpg.core/root) root-node)
    
    (def tm model)
    (frame :width 400
           :height 300
           :visible? true
           :content
           (scrollable
            (tree
             :renderer our-renderer
             :model tm
             :size [200 :by 300]
             :preferred-size [200 :by 300]
             :listen [:selection node-selected])))))
(adsasd)
