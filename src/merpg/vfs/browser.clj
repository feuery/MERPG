(ns merpg.vfs.browser
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer [simple-tree-model]]
            [merpg.vfs :refer :all]
            [merpg.UI.map-controller :refer [show]]
            
            [merpg.UI.main_layout] ;;This is required to let us play with its atoms :P
            [clojure.stacktrace :refer [print-stack-trace]]))

(def f (frame :width 800
              :height 600
              :visible? true))
(comment
  (defn render-file-item
  [renderer {:keys [value]}]
  (config! renderer :text (.getName value)
                   :icon (.getIcon chooser value))))

(defn get-content []
  (let [root-node (atom (make-directory [(nodify @merpg.UI.main-layout/map-set-image)]))
        model (simple-tree-model is-directory (fn [vec] vec) @root-node)]
    (println "Model " model)
    (tree :model model
          :background :red
          :renderer (fn [renderer {:keys [value]}]
                      (try
                        (let [nn (node-name value)]
                          (println "configing " nn)
                          (config! renderer :text nn))
                        (catch AssertionError ex
                          (print-stack-trace ex)))))))
          
