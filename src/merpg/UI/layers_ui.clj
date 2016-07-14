(ns merpg.UI.layers-ui
  (:require [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.mutable.to-registry-binding :as tbr]
            [seesaw.core :refer :all]
            [merpg.mutable.registry :as re]
            [merpg.mutable.layers :refer [layer-metas-ui] :as l]))

(defmethod popupmenu :layer [val]
  (popup :items [(menu-item :text "Remove layer"
                            :listen
                            [:action (fn [_]
                                       (let [selected-layer (-> :selected-layer
                                                                re/peek-registry
                                                                re/peek-registry)]
                                         (when (confirm (str "You're about to IRRECOVERABLY delete layer with a name " (:name selected-layer) "\n\nDo you wish to proceed?"))
                                           (re/remove-element! (re/peek-registry :selected-layer))
                                           (let [new-layer-id (-> @layer-metas-ui
                                                                  first
                                                                  first)]
                                             (re/register-element! :selected-layer new-layer-id)))))])
                 (menu-item)
                 (menu-item :text "Move up"
                            :listen
                            [:action (fn [_]
                                       (let [selected-layer (re/peek-registry :selected-layer)
                                             selected-order (-> selected-layer
                                                                re/peek-registry
                                                                :order)
                                             orders (->> @l/layer-metas-ui
                                                         (map second)
                                                         (map :order))
                                             max-ord (apply max orders)]
                                         (when (<= (inc selected-order) max-ord)
                                           (when (< (inc selected-order) max-ord)
                                             (if-let [upper-id (->> @l/layer-metas-ui
                                                                    (filter #(-> % second :order (= (inc selected-order))))
                                                                    (map first)
                                                                    first)]
                                               ;; lower the upper
                                               (re/update-registry upper-id
                                                                   (update upper-id :order dec))))
                                           ;; raise the lower
                                           
                                           (re/update-registry selected-layer
                                                               (update selected-layer :order inc)))))])
                 (menu-item :text "Move down"
                            :listen
                            [:action (fn [_]
                                       (let [selected-layer (re/peek-registry :selected-layer)
                                             selected-order (-> selected-layer
                                                                re/peek-registry
                                                                :order)]
                                         (when (>= (dec selected-order) 0)
                                           (when (> (dec selected-order) 0)
                                             (if-let [lower-id (->> @l/layer-metas-ui
                                                                    (filter #(-> % second :order (= (dec selected-order))))
                                                                    (map first)
                                                                    first)]
                                               (re/update-registry lower-id
                                                                   (update lower-id :order inc))))
                                           (re/update-registry selected-layer
                                                               (update selected-layer :order dec)))))])
                 (menu-item :text "Properties"
                            :listen
                            [:action (fn [_]
                                       (let [id (:id val)
                                             atom (tbr/atom->registry-binding id)]
                                         (swap! atom assoc :meta
                                                {:opacity
                                                 {:max 255}
                                                 :order
                                                 {:max (count (re/children-of! (:parent-id val)))}
                                                 :parent-id {:visible? false}
                                                 :subtype {:visible? false}
                                                 :type {:visible? false}
                                                 :id {:visible? false}})
                                         (ask-box atom)))])]))
