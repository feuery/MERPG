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

(defn atom-to-jlist [a & {:keys [transformator
                                 selected-index-atom]
                          :or [transformator identity
                               selected-index-atom (atom 0)]}]
  (let [list (listbox :model []
                      :listen [:mouse-clicked #(let [selected-id (first (selection %))]
                                                 (reset! selected-index-atom selected-id))]
                      :renderer (fn [rndr model]
                                  (config! rndr :text (-> model :value second :name))))]
    (add-watch a :bindable-jlist #(.setModel list (list-model %4 transformator)))
    (.setModel list (list-model @a transformator))
    list))
