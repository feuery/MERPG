(ns merpg.vfs.browser
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer [simple-tree-model]]
            [merpg.vfs :refer :all]
            [merpg.vfs.icon :refer :all]
            [merpg.UI.map-controller :refer [show]]
            
            [merpg.UI.main_layout] ;;This is required to let us play with its atoms :P
            [merpg.immutable.basic-map-stuff :refer [make-map]]
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

(def root (atom (make-directory (node-name (make-map 10 10 2) "Maps"))
                :validator
                #(not (some nil? (map meta %)))))

(comment
  Let the root contain N atoms, one for maps, one for anims, one for everything...
  These root's childs are the dirs, and all the grandchildren are files
  Clicking on a child-dir opens the browser there, and clicking on a child-file opens that file on the correct editor)

(defn push! [stack-atm element]
  (swap! stack-atm conj element))

(defn pop! [stack-atm]
  (let [last (last @stack-atm)]
    (swap! stack-atm drop-last)
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
                                                 (when-not (is-directory child)
                                                   (println (class child))
                                                   (println "child not dir"))
                                                 (when (is-directory child)
                                                   (push! folder-stack @root-atom)
                                                   (reset! root-atom child)))
                                               :title-transform (fn [elem]
                                                                  (try
                                                                    (node-name elem)
                                                                    (catch AssertionError ex
                                                                      (println "node-name hajos elementillä " (meta elem))
                                                                      (print-stack-trace ex))))))))
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

               
                 
    
