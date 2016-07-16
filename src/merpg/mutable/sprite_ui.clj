(ns merpg.mutable.sprite-ui
  (:require [seesaw.core :refer :all]
            [merpg.mutable.to-registry-binding :as tbr]
            [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]))

(defmethod popupmenu :sprite [val]
  (popup :items [(menu-item :text "Properties"
                            :listen
                            [:action (fn [_]
                                       (let [id (:id val)
                                             atom (tbr/atom->registry-binding id)]
                                         (swap! atom assoc :meta
                                                {:parent-id {:visible? false}
                                                 :type {:visible? false}
                                                 :id {:visible? false}})
                                         (ask-box atom)))])]))
