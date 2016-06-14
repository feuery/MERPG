(ns merpg.mutable.tileset
  (:require [reagi.core :as r]

            [merpg.IO.tileset :refer :all]
            [merpg.2D.core :refer [image]]
            [merpg.mutable.registry :as re]
            [merpg.events.mouse :as m]))

(defn tileset! [path]
  (re/register-element! (keyword (gensym "TILESET__")) {:name "New tileset"
                                                       :images (load-tileset path)
                                                       :type :tileset}))

(re/register-element! :initial {:name "Initial tileset"
                               :images (load-tileset (image 50 50 :color "#0000FF"))
                               :type :tileset})
(re/register-element! :selected-tileset :initial)

(def tileset-events (->> m/mouse-events
                         (r/filter #(= (:source %) :tileset-controller))
                         (r/map (fn [{:keys [tile-x tile-y]}]
                                  (re/update-registry :selected-tile
                                                      (assoc :selected-tile
                                                             :x tile-x
                                                             :y tile-y
                                                             :tileset (re/peek-registry :selected-tileset)))))))
