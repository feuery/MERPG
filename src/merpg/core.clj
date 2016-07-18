(ns merpg.core
  (:require [merpg.UI.main-layout :refer [show-mapeditor]]
            [seesaw.core :refer [native!]]
            [merpg.mutable.tileset]
            [merpg.mutable.maps]
            [merpg.mutable.registry :as re]
            [merpg.settings.core :as settings]
            [merpg.settings.nrepl])
  (:gen-class))

(settings/fire-events!)

(defn -main [& args]
  (native!)
  (reset! re/render-allowed? true)
  (swap! re/registry identity)
  (show-mapeditor))

(defn main []
  (-main))

(main)
