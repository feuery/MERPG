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
                                                 :id {:visible? false}
                                                 :frames {:visible? false}
                                                 :x {:visible? false}
                                                 :y {:visible? false}
                                                 :last-updated {:visible? false}
                                                 :frame-count {:visible? false}
                                                 :subtype {:visible? false}
                                                 :frame-index {:visible? false}
                                                 :frame-age {:max 1000}})
                                         (ask-box atom)))])]))
