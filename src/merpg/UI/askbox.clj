(ns merpg.UI.askbox
  (:require [clojure.pprint :refer [pprint]]
            [seesaw.core :refer :all]
            [seesaw.bind :as b]
            [clojure.core.async :as a]
            [merpg.UI.events :refer [allow-events]]
            [merpg.settings.core :refer [set-prop!]]
            [merpg.util :refer :all]))

(defn numeric-input [data-atom key-to-bind & {:keys [meta
                                                     settings-vm?] :or {meta {:max 255
                                                                              :min 0}
                                                                        settings-vm? false}}]
  (let [{:keys [max min] :or {max 255
                              min 0}} meta
        val (long (get @data-atom key-to-bind))
        val (if (< val min)
              min
              (if (> val max)
                max
                val))
        s (spinner :model (spinner-model val :from min :to max :by 1))]
    (b/bind s (b/b-swap! data-atom #(do
                                      (allow-events
                                      (if settings-vm?
                                        (set-prop! key-to-bind %2)
                                        (assoc %1 key-to-bind %2))))))
    (flow-panel :items [(str min) s (str max)])))

(defn ask-box
  "Constructs a dialog with a grid with viewmodel-atom's values presented as editable components. Edits are applied immediately. If viewmodel-atom has a :meta - key, there you can put constraints:

For numeric data, assuming data 
{:name \"Me\"
 :age 22}

the following viewmodel-atom
{:name \"Me\"
 :age 22
 :meta
  {:age 
   {:min 0
    :max 30}}} 

would limit the key :age in-between 0 and 30. 

Currently supported meta constraints:

:max, :min - limit numeric input range - applies to Integers and Longs
:visible? - hide the component completely - applies to everything"
  [viewmodel-atom & {:keys [visible?
                            completed-chan
                            settings-vm?]
                     :or {visible? true
                          completed-chan (a/chan)
                          settings-vm? false}}]
  (let [{:keys [meta]} @viewmodel-atom
        gridded-widgets (concat
                         (->> @viewmodel-atom
                              (filter (fn [[_ val]]
                                        (in? [java.lang.String
                                              java.lang.Integer
                                              java.lang.Long
                                              java.lang.Boolean
                                              clojure.lang.Keyword
                                              clojure.lang.PersistentVector] (class val))))
                              (mapv (fn [[key val :as asd]]
                                      (let [component-meta (get meta key)
                                            {:keys [visible?] :or {visible? true}} component-meta]
                                        (if visible?
                                          [(str key)
                                           (condp = (class val)
                                             java.lang.String (text :text (str val)
                                                                    :listen
                                                                    [:key-released (fn [e]
                                                                                     (allow-events
                                                                                      (if settings-vm?
                                                                                        (set-prop! key (text e))
                                                                                        (swap! viewmodel-atom assoc key (text e)))))])
                                             ;; binds itself to the atom
                                             java.lang.Long (numeric-input viewmodel-atom key
                                                                           :meta (get meta key)
                                                                           :settings-vm? settings-vm?)
                                             ;; binds itself to the atom
                                             java.lang.Integer (numeric-input viewmodel-atom key
                                                                              :meta (get meta key)
                                                                              :settings-vm? settings-vm?)
                                             java.lang.Boolean (checkbox :selected? val
                                                                         :listen
                                                                         [:item-state-changed (fn [e]
                                                                                                (allow-events
                                                                                                 (if settings-vm?
                                                                                                   (set-prop! key (selection e))
                                                                                                   (swap! viewmodel-atom assoc key (selection e)))))])
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
                                                            (allow-events
                                                             (if settings-vm?
                                                               (set-prop! key (selection e))
                                                               (swap! viewmodel-atom assoc key (selection e)))))])))]))))
                              (filter some?)
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
