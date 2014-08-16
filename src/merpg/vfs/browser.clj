(ns merpg.vfs.browser
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer [simple-tree-model]]
            [merpg.vfs :refer :all]
            [merpg.vfs.icon :refer :all]
            [merpg.UI.map-controller :refer [show]]
            
            [merpg.UI.main_layout] ;;This is required to let us play with its atoms :P
            [clojure.stacktrace :refer [print-stack-trace]]
            [merpg.util :refer [eq-gensym]]))

(def f (frame :width 800
              :height 600
              :visible? true))
(comment
  (defn render-file-item
  [renderer {:keys [value]}]
  (config! renderer :text (.getName value)
                   :icon (.getIcon chooser value))))

(def root (atom (make-directory (node-name (nodify @merpg.UI.main-layout/map-set-image) "Maps"))
                :validator
                #(not (some nil? (map meta %)))))

(defn push! [stack-atm element]
  (swap! stack-atm conj element))

(defn pop! [stack-atm]
  (let [last (last @folder-stack)]
		     (swap! folder-stack drop-last)
		     last))

(defn get-content [root-atom]
  (def folder-stack (atom []))
  (let [toret (grid-panel :columns 3)
        build-items (fn [root-nonatom]
                      (->> root-nonatom
                           (map (fn [node]
                                  (let [is-dir? (is-directory node)]
                                    (make-icon root-atom (node-id node)
                                               :on-click
                                               (fn [child]
                                                 (push! folder-stack @root-atom)
                                                 (reset! root-atom child))
                                               :title-transform (fn [elem]
                                                                 (println "%: " (meta elem))
                                                                 (node-name elem))))))
                           vec))
        real-builder (fn [_ _ _ new]
                       (config! toret :items (build-items @root-atom)))]
    (add-watch root-atom :explorer-watcher real-builder)
    (vertical-panel
     :items
     [(button :text "Go up the stack"
              :listen
              [:action
               (fn [_]
                 (reset! root-atom (pop! folder-stack)))])
    (real-builder nil nil nil @root-atom)])))
               
                 
    
