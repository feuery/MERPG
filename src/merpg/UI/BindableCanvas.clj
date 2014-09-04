(ns merpg.UI.BindableCanvas
  (:require [seesaw.core :refer :all]
            [merpg.IO.tileset :refer :all]
            [clojure.java.io :refer [file]]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer [pprint]])
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
  (let [;;Tehdään tästä joku agenttiviritys...
        img-provider (atom (future (img-transformer-fn @data-atom)))
        old-image (atom nil)
        c (canvas :paint (fn [_ g]
                           (try
                             (when (or (realized? @img-provider)
                                     (nil? @old-image))
                             (reset! old-image @@img-provider)
                             (reset! img-provider (future (img-transformer-fn @data-atom))))
                             (catch Exception ex
                               (println "Image provider (" (class @@img-provider) "): ")
                               (pprint @img-provider)
                               (print-stack-trace ex)))

                           (when-not (nil? @old-image)
                             (.drawImage g @old-image 0 0 nil))))]

    (doseq [d rest-to-bind]
      (add-watch d :bindable-canvas-updater (fn [_ _ _ _]
                                              (repaint! c))))
    (add-watch data-atom :bindable-canvas-updater (fn [_ _ _ da]
                                                    (println "Data-atomn of bindable-canvas changed")
                                                    (pprint da)
                                                    (repaint! c)))
    c))

