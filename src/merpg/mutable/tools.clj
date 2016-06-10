(ns merpg.mutable.tools
  (:require [reagi.core :as r]
            [clojure.core.async :as a]
            
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]))

(defn deftool
  "There's yet no api for tools decided"
  [id fn]
  (re/register-element! id {:name (str id)
                            :fn fn}))

;; registry<->gui - binding follows...

(defmacro make-atom-binding
  "Creates a reagi event stream with the name \"$id-view\" and an atom with the name \"$id-ui\" where the changes in the event stream will be propagated. This is backed by an core.async channel. This hack exists because seesaw.bind can't bind to an event stream but needs something one can set watches on. "
  [id & forms]
  (let [view-name (-> id (str "-view") symbol)
        atom-name (-> id (str "-ui") symbol)]
    `(do
       (def ~view-name ~@forms)
       (def ~atom-name (atom nil))
       (def ch# (a/chan))
       (r/subscribe ~view-name ch#)
       (a/go-loop [data# (a/<! ch#)]
         (when (some? data#)
           (reset! ~atom-name data#)
           (recur (a/<! ch#)))))))

(make-atom-binding selected-tool
                   (->> rv/local-registry
                        (r/map :selected-tool)
                        (r/filter some?)))
