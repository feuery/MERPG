(ns merpg.UI.BindableList
  (:require [seesaw.core :refer :all :exclude [width height]]
            [seesaw.bind :as b]
            [clojure.pprint :refer :all]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.mutable.to-registry-binding :as trb]))

(defn list-model 
  "Create list model based on collection"
  [items]
  (let [model (javax.swing.DefaultListModel.)]
    (doseq [item items] (.addElement model item))
    model))

(defn atom-to-jlist [a & {:keys [key]
                          :or [key nil]}]
  (let [selected-index-atom (if (some? key)
                              (trb/atom->registry-binding key)
                              (atom 0))
        list (listbox :model []
                      :listen [:mouse-clicked #(let [times-clicked (.getClickCount %)]
                                                 (condp = times-clicked
                                                   1 (let [selected-id (first (selection %))]
                                                       (reset! selected-index-atom selected-id))
                                                   2 (let [atom (trb/atom->registry-binding @selected-index-atom)]
                                                       (pprint atom)
                                                       (ask-box atom))))]
                      :renderer (fn [rndr model]
                                  (config! rndr :text (-> model :value second :name))))]
    (add-watch a :bindable-jlist #(.setModel list (list-model %4)))
    (.setModel list (list-model @a))
    list))
