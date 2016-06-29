(ns merpg.UI.maps-ui
  (:require [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.mutable.registry :as re]
            [merpg.mutable.to-registry-binding :as tbr]
            [seesaw.core :refer :all]
            [clojure.core.async :as a]
            [merpg.mutable.maps :refer [map! map-metas-ui]]
            [merpg.mutable.layers :refer [layer! mapwidth! mapheight! layer-count!]]))

(defmethod popupmenu :map [val]
  (popup :items [(menu-item :text "New layer"
                            :listen
                            [:action (fn [_]
                                       (let [smap (re/peek-registry :selected-map)]
                                         (layer! (mapwidth! smap)
                                                 (mapheight! smap)
                                                 :parent-id smap
                                                 :order (inc (layer-count! smap)))))])
                 (menu-item)
                 (menu-item :text "Remove map"
                            :listen
                            [:action (fn [_]
                                       
                                       (let [selected-map (-> :selected-map
                                                              re/peek-registry ;; id
                                                              re/peek-registry)] ;;data
                                         (when (confirm (str "You're about to IRRECOVERABLY delete map with a name " (:name selected-map) "\n\nDo you wish to proceed?"))
                                           (re/remove-element! (re/peek-registry :selected-map))
                                           (let [new-map-id (-> @map-metas-ui
                                                                first
                                                                first)]
                                             (re/register-element! :selected-map new-map-id)))))])
                 (menu-item)
                 (menu-item :text "Properties"
                            :listen
                            [:action (fn [_]
                                       (let [id (:id val)
                                             atom (tbr/atom->registry-binding id)]
                                         (ask-box atom)))])]))

