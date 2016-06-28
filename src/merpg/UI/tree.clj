(ns merpg.UI.tree
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer :all]
            [seesaw.dev :refer :all]
            [seesaw.mouse :refer [location]]
            [clojure.core :refer :all]
            [clojure.pprint :refer :all]
            [clojure.string :as str]
            [merpg.mutable.registry :as re])
  (:import [javax.swing.tree DefaultTreeModel DefaultMutableTreeNode]))

(defn- our-renderer [renderer {:keys [value] :as wut}]
  (try    
    (let [userobj (.getUserObject value)
          text (str (or
                     (:name userobj)
                     userobj))]
      (config! renderer :text text))
    (catch Exception ex
      ;; #break
      (pprint value)
      (throw ex))))

(defn. create [title parent-node]
  (try
    (let [child (DefaultMutableTreeNode. title)]
      (when-not (nil? parent-node)
        (.insert parent-node child 0))
      child)
    (catch NullPointerException ex
      #break
      (println "wtf?"))))
    

(defn- build-model! [model children parent]
  (doseq [[id val] children]
      (let [node (create val parent)
            new-children (re/children-of! id :exclude-types [:tile])]
        (if-not (empty? new-children)
          (build-model! model new-children node)))))

(defn node-selected [e]
  ;; To get the metadata of the selected object use
  ;; (-> e seesaw.core/selection last .getUserObject meta)
  (when (javax.swing.SwingUtilities/isRightMouseButton e)
    (let [src (to-widget e)
          [x y] (location e)
          selRow (.getRowForLocation src x y)
          selPath (.getPathForLocation src x y)]
      
      (.setSelectionPath src selPath)
      (if (> selRow -1)
        (.setSelectionRow src selRow)))))
      
      ;; (.show popmenu src x y))))
    ;; (-> e selection last .getUserObject pr-str alert)

(defn domtree []
  (let [root-node (create ":root" nil)
        model (DefaultTreeModel. root-node)]
    (build-model! model (re/children-of! :root) root-node)
    (tree
     :renderer our-renderer
     :model model
     :size [200 :by 300]
     :preferred-size [200 :by 300]
     :listen [:mouse-clicked node-selected])))
