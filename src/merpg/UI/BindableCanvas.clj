(ns merpg.UI.BindableCanvas
  (:require [seesaw.core :refer :all]
            [merpg.IO.tileset :refer :all]
            [clojure.java.io :refer [file]])
  (:import [javax.imageio ImageIO]))

(defn- save-img [img path]
  (ImageIO/write img "png" (file path)))

;;Piccolo2d?

(defn is-seq? [thing]
  (try
    (let [_ (seq thing)]
      true)
    (catch Exception ex
      false)))

(defn bindable-canvas [data-atom img-transformer-fn & {:keys [rest-to-bind]
                                                       :or {rest-to-bind []}}]
  (let [c (canvas :paint (fn [_ g]
                           (.drawImage g (img-transformer-fn @data-atom) 0 0 nil)))]

    (doseq [d rest-to-bind]
      (add-watch d :bindable-canvas-updater (fn [_ _ _ _]
                                              (repaint! c))))
    (add-watch data-atom :bindable-canvas-updater (fn [_ _ _ _]
                                                    (repaint! c)))
    c))

