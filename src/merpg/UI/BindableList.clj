(ns merpg.UI.BindableList
  (:require [seesaw.core :refer :all :exclude [width height]]
            [seesaw.bind :as b]
            [clojure.pprint :refer :all]))

(defn list-model 
  "Create list model based on collection"
  [items transformator]
  (let [model (javax.swing.DefaultListModel.)]
    (doseq [item items] (.addElement model item))
    model))

;; (defn render-fn [renderer info]
;;   (let [v (:value info)]
;;     (apply config! renderer 
;;       (if (even? v) 
;;         [:text (str v " is even") :font even-font :foreground "#000033"]
;; [:text (str v " is odd") :font odd-font :foreground "#aaaaee"]))))

(defn atom-to-jlist [a & {:keys [transformator
                                 selected-index-atom]
                          :or [transformator identity
                               selected-index-atom (atom 0)]}]
  ;; seesaw.cells/to-cell-renderer
  (let [list (listbox :model []
                      :listen [:selection #(do
                                             (reset! selected-index-atom (selection %)))]
                      :renderer (fn [rndr model]
                                  (config! rndr :text (-> model :value second :name))))]
    (add-watch a :bindable-jlist #(.setModel list (list-model %4 transformator)))
    (.setModel list (list-model @a transformator))
    list))
