(ns merpg.core
  (:require [merpg.immutable.basic-map-stuff :refer [make-map]]
            [merpg.UI.main-layout :refer [show-mapeditor]]
            [seesaw.core :refer [native!]])
  (:gen-class))

;; (try
;;   (println "UB: " merpg.core.project/uberjarring)
;;   (catch Exception exq
;;     (println "Probably not uberjarring")))

(defn -main [& args]
  (native!)
  (show-mapeditor))

(defn main []
  (-main))

;; (main)
