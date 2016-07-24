(ns merpg.reagi
  "Provides a wrapper for the reagi library that makes disabling streams easy"
  (:require [reagi.core :as r]
            [clojure.pprint :refer :all]))

(def editor-streams-running? (atom true))

(defmacro editor-stream [initial-expr & rest-exprs]
  `(->> ~initial-expr
        (r/filter (fn [~'_]
                    @editor-streams-running?))
        ~@rest-exprs))

;; (editor-stream
;;  (r/sample 1000 r/time)
;;  (r/map #(println "A second has passed, r/time is now " %)))
