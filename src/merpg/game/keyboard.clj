(ns merpg.game.keyboard
  (:require [reagi.core :as r]
            [merpg.reagi :refer :all])
  (:import [java.awt.event KeyEvent]))

(def keycodes-down (atom #{}))

(def keyboard-stream (game-stream (r/sample 100 keycodes-down)))

(defn for-keys-fn [keyset fn]
  (->> keyboard-stream
       (r/filter (partial every? keyset))
       (r/map fn)))

(defmacro for-keys [keyset [keyset-binding] & forms]
  (let [keyset-binding (or keyset-binding
                           'keyset)]
  `(for-keys-fn ~keyset
                (fn [~keyset-binding]
                  ~@forms))))
