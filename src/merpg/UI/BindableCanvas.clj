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
  (let [img-agent (agent (img-transformer-fn @data-atom)
                         :error-handler (fn [agent ex]
                                          (println "Img-agent blew up in bindable-canvas")
                                          (print-stack-trace ex)))
        old-image (atom nil)
        c (canvas :paint (fn canvas-painter [_ g]
                           (try
                             (.drawImage g @img-agent 0 0 nil)
                             (catch RuntimeException ex
                               (print-stack-trace ex)))))]

    (doseq [d rest-to-bind]
      (add-watch d :bindable-canvas-updater (fn secondary-binder [_ _ _ _]
                                              (send img-agent (fn [_] (img-transformer-fn @data-atom))))))
    (add-watch data-atom :bindable-canvas-updater (fn primary-binder [_ _ _ da]
                                                    (send img-agent (fn [_] (img-transformer-fn da)))))
    (add-watch img-agent :repainter (fn repainter [_ _ _ _]
                                      (try
                                        (repaint! c)
                                        (catch Exception ex
                                          (print-stack-trace ex)))))
    c))
