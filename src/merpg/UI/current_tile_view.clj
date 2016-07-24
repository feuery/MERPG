(ns merpg.UI.current-tile-view
  (:require [seesaw.core :refer :all]
            [merpg.UI.draggable-canvas :refer [draggable-canvas]]
            [merpg.mutable.tiles :refer [selected-tile add-current-tile-watcher
                                         remove-current-tile-watcher]]))

(defn current-tile-view []
  (let [c (config! (draggable-canvas :paint-fn (fn [_ g]
                                                 (if (realized? selected-tile)
                                                   (.drawImage g @selected-tile nil 0 0)))
                                     :frame-ms 1000)
                   :size [50 :by 50])]
    (scrollable c)))
