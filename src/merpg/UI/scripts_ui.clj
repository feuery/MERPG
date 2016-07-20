(ns merpg.UI.scripts-ui
  (:require [seesaw.core :refer :all]
            [merpg.mutable.to-registry-binding :as tbr]
            [merpg.mutable.registry :as re]
            [merpg.UI.events :as e]
            [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]))

(defmethod popupmenu :script [val]
  (popup :items [(menu-item :text "Remove script"
                            :listen
                            [:action (fn [_]
                                       (let [{:keys [id name]} val]
                                         (if (confirm (str "Do you really want to irrecoverably delete script called " name))
                                           (e/allow-events
                                            (re/remove-element! id)))))])
                                         
                 (menu-item :text "Properties"
                            :listen
                            [:action (fn [_]
                                       (let [id (:id val)
                                             atom (tbr/atom->registry-binding id)]
                                         ;; (swap! atom assoc :meta
                                         ;;        {:parent-id {:visible? false}
                                         ;;         :type {:visible? false}
                                         ;;         :id {:visible? false}})
                                         (ask-box atom)))])]))
