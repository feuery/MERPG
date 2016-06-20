(ns merpg.UI.askbox
  (:require [clojure.pprint :refer [pprint]]
            [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [clojure.core.async :as a]))

(defn in? [vec val]
  (some (partial = val) vec))
    

(defn numeric-input [min max data-atom key-to-bind]
  (let [val (get @data-atom key-to-bind)
        val (if (< val min)
              min
              (if (> val max)
                max
                val))
        s (slider :value val
                  :max max
                  :min min)
        t (text :text (str val)
                :editable? false
                :enabled? false
                :size [100 :by 30])]
    (b/bind s
            (b/tee 
             (b/b-swap! data-atom #(assoc %1 key-to-bind %2))
             (b/bind (b/transform str) t)))
    
    (flow-panel :items [(border-panel :center s
                                      :east (str max)
                                      :west (str min))
                        t])))

(def viewmodel-atom (atom {:asdf 0
                            :age "asd"
                            :jees? true}))

(defn ask-box [viewmodel-atom & {:keys [visible?
                                        completed-chan]
                                 :or {visible? true
                                      completed-chan (a/chan)}}]
  (let [gridded-widgets (concat
                         (->> @viewmodel-atom
                              (filter (fn [[_ val]]
                                        (in? [java.lang.String
                                              java.lang.Integer
                                              java.lang.Long
                                              java.lang.Boolean
                                              clojure.lang.Keyword
                                              clojure.lang.PersistentVector] (class val))))
                              (mapv (fn [[key val :as asd]]
                                      [(str key)
                                       (condp = (class val)
                                         java.lang.String (text :text (str val)
                                                                :listen
                                                                [:key-released (fn [e]
                                                                                 (swap! viewmodel-atom assoc key (text e)))])
                                         ;; binds itself to the atom
                                         java.lang.Long (numeric-input 0 255
                                                                       viewmodel-atom key)
                                         ;; binds itself to the atom
                                         java.lang.Integer (numeric-input 0 255
                                                                          viewmodel-atom key)
                                         java.lang.Boolean (checkbox :selected? val
                                                                     :listen
                                                                     [:item-state-changed (fn [e]
                                                                                            (swap! viewmodel-atom assoc key (selection e)))])
                                         clojure.lang.Keyword (text :text (str val)
                                                                    :editable? false
                                                                    :enabled? false)
                                         clojure.lang.PersistentVector
                                         (do
                                           (swap! viewmodel-atom assoc key (first val))
                                           (combobox :model val
                                                                                 :listen
                                                                                 [:selection
                                                                                  (fn [e]
                                                                                    (swap! viewmodel-atom assoc key (selection e)))])))]))
                              flatten
                              vec)
                         ["" (button :id :ok :text "Ok")])
        
        layout (grid-panel :columns 2
                           :items gridded-widgets)
        finished (atom false)
        f (frame :content layout
                 :visible? visible?
                 :on-close :dispose
                 :listen
                 [:window-closed (fn [_]
                                   (a/go
                                     (if-not @finished
                                       (a/>! completed-chan false))))])]
    (listen (select f [:#ok])
            :action (fn [_]
                      (a/go
                        (swap! finished not)
                        (a/>! completed-chan true))
                      (dispose! f)))
    (-> f
        pack!)
    completed-chan))
