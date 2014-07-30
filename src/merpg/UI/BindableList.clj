(ns merpg.UI.BindableList
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]))

(defn bindable-list [list-data-atom selected-item-atom &
                     {:keys [custom-model-bind]
                      :or {custom-model-bind nil}}]
  (defn create-renderer
    [renderer {:keys [value]}]
    (config! renderer :text (str (custom-model-bind value))))
  
  (let [list (listbox :model @list-data-atom
                      :renderer create-renderer)]
    (b/bind list-data-atom (b/property list :model))
    (b/bind (b/selection list) selected-item-atom)
    list))

(defn string-renderer [f]
  (default-list-cell-renderer
    (fn [this {:keys [value]}] (.setText this (str (f value))))))
