(ns merpg.UI.BindableList
  (:require [seesaw.core :refer :all :exclude [width height]]
            [seesaw.bind :as b]
            [clojure.pprint :refer :all]))

(defn list-model 
  "Create list model based on collection"
  [items transformator]
  (let [model (javax.swing.DefaultListModel.)]
    (doseq [item items] (.addElement model (transformator item)))
    model))

(defn atom-to-jlist [a & {:keys [transformator] :or [transformator identity]}]
  (let [list (listbox :model [])]
    (add-watch a :bindable-jlist #(.setModel list (list-model %4 transformator)))
    (.setModel list (list-model @a transformator))
    list))
