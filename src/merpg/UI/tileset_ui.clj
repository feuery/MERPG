(ns merpg.UI.tileset-ui
  (:require [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.mutable.registry :as re]
            [merpg.mutable.to-registry-binding :as tbr]
            [seesaw.core :refer :all]
            [clojure.core.async :as a]
            [merpg.mutable.maps :refer [map! map-metas-ui]]
            [merpg.mutable.layers :refer [layer! mapwidth! mapheight! layer-count!]]))

(defmethod popupmenu :tileset [val]
  (popup :items [(menu-item :text "Remove tileset"
                            :listen
                            [:action (fn [_]
                                       (let [selected-tileset (re/peek-registry :selected-tileset)]
                                         (if-not (= selected-tileset :initial)
                                           (let [{:keys [name]} (re/peek-registry selected-tileset)
                                                 tiles-to-update (->> @re/registry
                                                                      (filter (fn [[_ val]]
                                                                                (and
                                                                                 (= (:type val) :tile)
                                                                                 (= (:tileset val) selected-tileset))))
                                                                      (map first))]
                                             (when (confirm (str "You're about to IRRECOVERABLY delete tileset with a name " name "\n\nDo you wish to proceed?"))
                                               (re/remove-element! selected-tileset)
                                               (doseq [tile-id tiles-to-update]
                                                 (re/update-registry tile-id
                                                                     (assoc tile-id
                                                                            :x 0
                                                                            :y 0
                                                                            :tileset :initial)))))
                                           (alert "You can't remove tileset :initial"))))])
                 (menu-item)
                 (menu-item :text "Properties"
                            :listen
                            [:action (fn [_]
                                       (let [id (:id val)
                                             atom (tbr/atom->registry-binding id)]
                                         (swap! atom assoc :meta
                                                {:images {:visible? false}
                                                 :type {:visible? false}
                                                 :parent-id {:visible? false}
                                                 :id {:visible? false}})
                                         (ask-box atom)))])]))
