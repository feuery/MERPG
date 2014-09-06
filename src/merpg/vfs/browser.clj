(ns merpg.vfs.browser
  (:require [seesaw.core :refer :all]
            [seesaw.tree :refer [simple-tree-model]]
            [merpg.vfs.icon :refer :all]
            [merpg.UI.map-controller :refer [show]]
            
            [merpg.UI.main-layout :refer [show-mapeditor]]
            [merpg.immutable.basic-map-stuff :refer [make-map]]
            [clojure.stacktrace :refer [print-stack-trace]]
            [merpg.util :refer [eq-gensym]]
            [merpg.pxart.main-layout :refer [show-animation-editor make-frame-general]]
            [clojure.pprint :refer [pprint]]))

(comment
  (defn render-file-item
  [renderer {:keys [value]}]
  (config! renderer :text (.getName value)
                   :icon (.getIcon chooser value))))
(comment
  Let the root contain N atoms, one for maps, one for anims, one for everything...
  These root's childs are the dirs, and all the grandchildren are files
  Clicking on a child-dir opens the browser there, and clicking on a child-file opens that file on the correct editor)

;;Keys are the dir-names :P
(def root {:maps (atom [(make-map 10 10 2)])
           :animations (atom [])}) ;; (make-frame-general (atom []) 10 10 (range 0 4))])})

;(add-watch (:animations root) :vectorifier 

(defn get-content [root]
  (def folder-stack (atom []))
  (let [toret (grid-panel :columns 3)
        cwd-atom (atom root :validator (fn [new-cwd]
                                         (or (map? new-cwd)
                                             (vector? new-cwd))))
        build-grid (fn [_ _ _ new-cwd]
                     (config! toret :items
                              (if (map? new-cwd)
                                (->> new-cwd
                                     (map (fn [[key child :as tuple]]
                                            (println "tuple " tuple)
                                            (make-icon child true (str key)
                                                       :on-click
                                                       (fn [child]
                                                         (if-not (instance? clojure.lang.Atom child)
                                                           (throw (Exception. (str "child " (class child) " is (supposed) to be atom always at this point"))))
                                                           (case key
                                                             :maps (show-mapeditor child)
                                                             :animations (do
                                                                           (pprint @child)
                                                                           (println (class @child))
                                                                           (show-animation-editor child 1))
                                                             
                                                             (do
                                                               (alert (str "Type " key " not recognized"))))))))
                                     vec)))
                     toret)]
    (add-watch cwd-atom :cwd-browser-refresher build-grid)
    (vertical-panel
     :items
     [;; (button :text "Parent dir"
      ;;         :listen
      ;;         [:action (fn [_]
      ;;                    (when-let [new-cwd (pop! folder-stack)]
      ;;                      (reset! cwd-atom new-cwd)))])
      (build-grid nil nil nil @cwd-atom)])))

(def f (frame
        :width 800
        :height 600
        :visible? true
        :title "MERPG-browser"
        :content (get-content root)))
                   
