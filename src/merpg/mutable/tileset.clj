(ns merpg.mutable.tileset
  (:require [reagi.core :as r]

            [merpg.IO.tileset :refer :all]
            [merpg.UI.events :as e]
            [merpg.2D.core :refer [image]]
            [merpg.mutable.registry :as re]
            [merpg.events.mouse :as m]))

(defn tileset!
  ([path]
   (e/allow-events
    (re/register-element! (keyword (gensym "TILESET__")) {:name "New tileset"
                                                          :images (load-tileset path)
                                                          :parent-id :root
                                                          :type :tileset})))
  ([id name image]
   (e/allow-events
    (re/register-element! id {:name name
                              :images (img-to-tileset image)
                              :parent-id :root
                              :type :tileset}))))

(re/register-element! :initial {:name "Initial tileset"
                               :images (load-tileset (image 50 50 :color "#0000FF"))
                                :type :tileset
                                :parent-id :root})
(re/register-element! :selected-tileset :initial)

(def tileset-events (->> m/mouse-events
                         (r/filter #(= (:source %) :tileset-controller))
                         (r/map (fn [{:keys [tile-x tile-y]}]
                                  (re/update-registry :selected-tile
                                                      (assoc :selected-tile
                                                             :x tile-x
                                                             :y tile-y
                                                             :tileset (re/peek-registry :selected-tileset)))))))
