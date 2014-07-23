(ns merpg.UI.BindableCanvas
  (:require [seesaw.core :refer :all]
            [merpg.IO.tileset :refer :all]
            [clojure.java.io :refer [file]])
  (:import [javax.imageio ImageIO]))

(defn- save-img [img path]
  (ImageIO/write img "png" (file path)))

;;Piccolo2d?


(defn bindable-canvas [data-atom img-transformer-fn]
  (let [c (canvas :paint (fn [_ g]
                           (println "@bindable-canvas.:paint")
                           (.drawImage g (img-transformer-fn @data-atom) 0 0 nil)))]
    (add-watch data-atom :bindable-canvas-updater (fn [_ _ _ _]
                                                    (repaint! canvas)))
    c))

