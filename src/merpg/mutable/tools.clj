(ns merpg.mutable.tools
  (:require [reagi.core :as r]
            [clojure.core.async :as a]
            [clojure.pprint :refer [pprint]]
            
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]))

(defn deftool
  "The func shall receive the tile-id of the tile it's been used on as a parameter. This will probably break with the hitlayer-tool, but that'll be tested when the time is right"
  [id func]
  (re/register-element! id {:name (str id)
                            :type :tool
                            :fn func}))

(deftool :pen (fn [tile-id]
                (let [selected-tile (re/peek-registry :selected-tile)]
                  (println "tile-id " tile-id)
                  (pprint selected-tile)
                  (re/update-registry tile-id
                                      (let [tr (assoc tile-id
                                                      :x (:x selected-tile)
                                                      :y (:y selected-tile)
                                                      :tileset (:tileset selected-tile)
                                                      :rotation 0)]
                                        tr)))))

(deftool :hit-tool (fn [tile-id]
                     (re/update-registry tile-id
                                         (update tile-id :can-hit? not))))

(deftool :rotater (fn [tile-id]
                    (re/update-registry tile-id
                                        (update tile-id :rotation #(mod (inc %) 4)))))
                                                                     
                      

;; registry<->gui - binding follows...

(defmacro make-atom-binding
  "Creates a reagi event stream with the name \"$id-view\" and an atom with the name \"$id-ui\" where the changes in the event stream will be propagated. This is backed by an core.async channel. This hack exists because seesaw.bind can't bind to an event stream but needs something one can set watches on. "
  [id {:keys [allow-seq?] :or [allow-seq? true]} & forms]
  (let [view-name (-> id (str "-view") symbol)
        atom-name (-> id (str "-ui") symbol)
        atom-str (str atom-name)
        atom-script `(def ~atom-name (atom nil :validator
                                           ~(if-not allow-seq?
                                               `(complement coll?)
                                               `(constantly true))))]
                                                   
    `(do
       (def ~view-name ~@forms)
       ~atom-script
       (def ch# (a/chan))
       (r/subscribe ~view-name ch#)
       (a/go-loop []
         
           (if-let [asd# (a/<! ch#)]
             (do
               (reset! ~atom-name asd#)
               (recur))
             (println "it seems we got a nil on " ~atom-str))))))

;; (make-atom-binding selected-tool {:allow-seq? true}
;;                    (->> rv/local-registry
;;                         (r/filter #(and (coll? %)
;;                             (contains? % :selected-tool)))
;;                         (r/map :selected-tool)))


(make-atom-binding all-tools {:allow-seq? true}
                   (->> rv/local-registry
                        (r/map (fn [r]
                                 (->> r
                                      (filter #(= (-> % second :type) :tool))
                                      (mapv first))))))

(def selected-tool-view (->> rv/local-registry
                             (r/filter #(and (coll? %)
                                             (contains? % :selected-tool)))
                             (r/map :selected-tool)))

(def c (a/chan))
(def selected-tool-ui (atom :nil :validator (complement coll?)))

(r/subscribe selected-tool-view c)

(a/go-loop [data (a/<! c)]
  (when (some? data)
    (reset! selected-tool-ui data)
    (recur (a/<! c))))

(println "tools.clj loaded")
