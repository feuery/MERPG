(ns merpg.mutable.tileset
  (:require [merpg.IO.tileset :refer :all]
            [merpg.mutable.registry :as r]))

(defn tileset! [path]
  (r/register-element! (keyword (gensym "TILESET__")) {:name "New tileset"
                                                       :images (load-tileset path)
                                                       :type :tileset}))
