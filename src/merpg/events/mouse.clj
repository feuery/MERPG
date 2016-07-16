(ns merpg.events.mouse
  (:require [reagi.core :as r]
            [schema.core :as s]
            [merpg.UI.askbox :refer [in?]]))

(def type-pred (s/pred (partial in? [:mousedown :mousemove :mouseup])))

(def mouse-event-schema {:pixel-x s/Int
                         :pixel-y s/Int

                         :tile-x s/Int
                         :tile-y s/Int
                         :source s/Keyword
                         :process? s/Bool
                         :type type-pred})

;; r/filter runs the schema validation only now-and-then
;; who knows what's the matter with it

(def mouse-events (->> (r/events {:pixel-x 0 :pixel-y 0
                                  :tile-x 0 :tile-y 0
                                  :source :nil
                                  :process? false
                                  :type :mousedown})
                       (r/filter (partial s/validate mouse-event-schema))
                       (r/filter :process?)))

(s/defn ^:always-validate post-mouse-event! [pixel-x :- s/Int
                                             pixel-y :- s/Int
                                             source :- s/Keyword
                                             type :- type-pred]
  (r/deliver mouse-events {:pixel-x pixel-x
                           :pixel-y pixel-y
                           :tile-x (long (/ pixel-x 50))
                           :tile-y (long (/ pixel-y 50))
                           :source source
                           :process? true
                           :type type}))
