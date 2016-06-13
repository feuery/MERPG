(ns merpg.mutable.tileset
  (:require [merpg.IO.tileset :refer :all]
            [merpg.2D.core :refer [image]]
            [merpg.mutable.registry :as r]))

(defn tileset! [path]
  (r/register-element! (keyword (gensym "TILESET__")) {:name "New tileset"
                                                       :images (load-tileset path)
                                                       :type :tileset}))

(r/register-element! :initial {:name "Initial tileset"
                               :images (load-tileset (image 50 50 :color "#0000FF"))
                               :type :tileset})
(r/register-element! :selected-tileset :initial)
