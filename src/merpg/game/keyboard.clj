(ns merpg.game.keyboard
  (:require [reagi.core :as r]
            [merpg.reagi :refer :all]
            [clojure.pprint :refer :all])
  (:import [java.awt.event KeyEvent]))

;; #{set of KeyEvent constants} => fn
(def kbd-mapping (atom {}))

(defn add-event-mapping! [keyset fn]
  (swap! kbd-mapping assoc keyset fn))

(defn drop-event-mapping!
  ([keyset]
   (swap! kbd-mapping dissoc keyset))
  ([keyset1 & rst]
   (drop-event-mapping! keyset1)
   (doseq [ks rst]
     (drop-event-mapping! ks))))
   

(def keycodes-down (atom #{}))

(defn keycode-handler [keyset]

  (if-let [fun (get @kbd-mapping keyset)]
    (fun keyset)
    (if (> (count keyset) 1)
      (let [event-fns (->> keyset
                           (map #(get @kbd-mapping (hash-set %)))
                           (filterv some?))]
        (doseq [f event-fns]
          (f keyset))))))

(def keycode-stream (game-stream (r/sample 100 keycodes-down)
                                 (r/map (fn [keyset]
                                          (keycode-handler keyset)))))
