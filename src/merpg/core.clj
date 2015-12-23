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
  (def root {:maps (atom (with-meta [(make-map 10 10 1)]
                           {:tyyppi :root}))})
  (native!)
  (show-mapeditor (:maps root)))
