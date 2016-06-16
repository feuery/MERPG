(ns merpg.UI.askbox
  (:require [clojure.pprint :refer [pprint]]
            [seesaw.core :refer :all]
            [seesaw.bind :as b]))
    

(defn numeric-input [min max data-atom key-to-bind]
  (let [val (key-to-bind @data-atom)
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

(defn ask-box [viewmodel-atom & {:keys [visible?] :or {visible? true}}]
  
  (let [gridded-widgets (concat
                         (->> @viewmodel-atom
                              (mapv (fn [[key val]]
                                      [(str key)
                                       (condp = (class val)
                                         java.lang.String (text :text (str val)
                                                                :listen
                                                                [:key-released (fn [e]
                                                                                 (swap! viewmodel-atom assoc key (text e)))])
                                         ;; binds itself to the atom
                                         java.lang.Long (numeric-input 0 100
                                                                       viewmodel-atom key)
                                         ;; binds itself to the atom
                                         java.lang.Integer (numeric-input 0 100
                                                                          viewmodel-atom key)
                                         java.lang.Boolean (checkbox :selected? val
                                                                     :listen
                                                                     [:item-state-changed (fn [e]
                                                                                            (swap! viewmodel-atom assoc key (selection e)))]))]))
                              flatten
                              vec)
                         ["" (button :id :ok :text "Ok")])
        
        layout (grid-panel :columns 2
                           :items gridded-widgets)
        f (frame :content layout
                 :visible? visible?
                 :on-close :dispose)]
    (listen (select f [:#ok])
            :action (fn [_]
                      (dispose! f)))
    (-> f
        pack!)))
