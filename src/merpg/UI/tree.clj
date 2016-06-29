(ns merpg.UI.tree
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer :all]
            [seesaw.dev :refer :all]
            [seesaw.mouse :refer [location]]
            [clojure.core :refer :all]
            [clojure.pprint :refer :all]
            [clojure.string :as str]
            [merpg.mutable.registry :as re]
            [merpg.macros.multi :refer :all]
            [reagi.core :as r]
            [merpg.mutable.registry-views :as rv])
  (:import [javax.swing.tree DefaultTreeModel DefaultMutableTreeNode]))

(def-real-multi popupmenu [val] (or
                                 (:type val)
                                 val))

(defmethod popupmenu nil [val]
  (alert (str "No popupmenu defined for " (pr-str val) " (" (class val) ")")))

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

(defn- create [title parent-node]
  (try
    (let [child (DefaultMutableTreeNode. title)]
      (when-not (nil? parent-node)
        (.insert parent-node child 0))
      child)
    (catch NullPointerException ex
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
        (.setSelectionRow src selRow))
      (let [selected-object (-> src selection
                                last
                                .getUserObject)
            popup (popupmenu selected-object)]
        (if (some? popup)
          (.show popup src x y))))))

(def domtree-updaters (atom []))

(def model (->> rv/local-registry
                (r/map (fn [r]
                         (let [root-node (create ":root" nil)
                               model (DefaultTreeModel. root-node)]
                           (build-model! model (re/children-of r :root) root-node)
                           model)))
                (r/map (fn [r]
                         (doseq [f @domtree-updaters]
                           (f))
                         r))))

(defn domtree []
  (let [t (tree
           :renderer our-renderer
           :model @model
           :size [200 :by 300]
           :preferred-size [200 :by 300]
           :listen [:mouse-clicked node-selected])]
    (swap! domtree-updaters conj (fn []
                                   (config! t :model @model)))
    t))
