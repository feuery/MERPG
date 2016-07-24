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

(def game-streams-running? (atom false))

(defmacro game-stream [initial-expr & rest-exprs]
  `(->> ~initial-expr
        (r/filter (fn [~'_]
                    @game-streams-running?))
        ~@rest-exprs))

(defmacro stream
  "An OR of editor- and game-streams-running?"
  [initial-expr & rest-exprs]
  `(->> ~initial-expr
        (r/filter (fn [~'_]
                    (or @editor-streams-running?
                        @game-streams-running?)))
        ~@rest-exprs))
