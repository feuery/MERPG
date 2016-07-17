(ns merpg.UI.spritesheet-popupmenu
  (:require [seesaw.core :refer :all]
            [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.mutable.to-registry-binding :as tbr]
            [merpg.mutable.registry :as re]
            [merpg.UI.events :as e]))

(defmethod popupmenu :sprite [val]
  (popup :items [(menu-item :text "Remove"
                            :listen
                            [:action (fn [_]
                                       (e/allow-events
                                        (re/remove-element! (:id val))))])
                 (menu-item :text "Properties"
                            :listen
                            [:action (fn [_]
                                       (let [id (:id val)
                                             atom (tbr/atom->registry-binding id)]
                                         (swap! atom assoc :meta
                                                {:parent-id {:visible? false}
                                                 :type {:visible? false}
                                                 :id {:visible? false}})
                                         (ask-box atom)))])]))
