(ns merpg.settings.nrepl
  (:require [merpg.settings.core :refer :all]))

(add-prop-watch :nrepl-running? :start-nrepl
                (fn [start?]
                  (if start?
                    (println "Starting nrepl...")
                    (println "Stopping nrepl..."))))
