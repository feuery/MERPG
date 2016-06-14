(ns merpg.UI.current-tile-view
  (:require [seesaw.core :refer :all]
            [merpg.mutable.tiles :refer [selected-tile add-current-tile-watcher
                                         remove-current-tile-watcher]]))

(defn current-tile-view []
  (let [c (canvas :paint (fn [_ g]
                           (if (realized? selected-tile)
                             (.drawImage g @selected-tile nil 0 0))))]
    (remove-current-tile-watcher :current-tile)
    (add-current-tile-watcher #(repaint! c) :current-tile)
    (scrollable c)))
