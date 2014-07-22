(ns merpg.UI.BindableCanvas
  (:require [seesaw.core :refer :all]
            [merpg.IO.tileset :refer :all]
            [clojure.java.io :refer [file]])
  (:import [javax.imageio ImageIO])))

(defn- save-img [img path]
  (ImageIO/write img "png" (file path))))

;;Piccolo2d?


(defn bindable-canvas [bufferedimage]
  (let [c (canvas :paint (fn [_ g]
                           (.drawImage g bufferedimage 0 0 nil))
                  :background :red)]
    (timer (fn [_] (repaint! c) 1000))
    c)))

(defn- show [f stuff]
  (config! f :content stuff))
