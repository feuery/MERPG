(ns merpg.UI.BindableCanvas
  (:require [seesaw.core :refer :all]
            [merpg.IO.tileset :refer :all]
            [merpg.2D.core :refer [img-width img-height]]
            [clojure.java.io :refer [file]]
            [clojure.core :refer :all]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.pprint :refer [pprint]])
  (:import [javax.imageio ImageIO]
           [javax.swing JComponent Scrollable]
           [java.awt Dimension]
           [merpg.java BindableCanvas]))

(defn- save-img [img path]
  (ImageIO/write img "png" (file path)))

;;Piccolo2d?

(defn is-seq? [thing]
  (try
    (let [_ (seq thing)]
      true)
    (catch Exception ex
      false)))

(extend BindableCanvas 
      seesaw.config/Configurable
      {:config!* (fn [target args]
                   (try
                     (doseq [[k v] (partition 2 args)]
                       (.put (.statemap target) k v))
                     (catch UnsupportedOperationException ex
                       (pprint args)
                       (throw ex))))
       :config* (fn [target Name]
                  (.get (.statemap target) Name))})

(defn bindable-canvas [data-atom img-transformer-fn & {:keys [rest-to-bind]
                                                       :or {rest-to-bind []}}]
  (let [img-agent (agent (img-transformer-fn @data-atom)
                         :error-handler (fn [agent ex]
                                          (println "Img-agent blew up in bindable-canvas")
                                          (print-stack-trace ex)))
        old-image (atom nil)
        c (BindableCanvas. img-agent)
        scroller (scrollable c)]
    (config! c :preferred-size [(img-width @img-agent) :by (img-height @img-agent)])

    (doseq [d rest-to-bind]
      (add-watch d :bindable-canvas-updater (fn secondary-binder [_ _ _ _]
                                              (send img-agent (fn [_] (img-transformer-fn @data-atom))))))
    (add-watch data-atom :bindable-canvas-updater (fn primary-binder [_ _ _ da]
                                                    (send img-agent (fn [_] (img-transformer-fn da)))))
    (add-watch img-agent :repainter (fn repainter [_ _ _ _]
                                      (try
                                        (println "Img-agentin koko: " [(img-width @img-agent) :by (img-height @img-agent)])
                                        (println "Img-agent changed!")
                                        
                                        (.invalidate c)
                                        (.invalidate scroller)
                                        (repaint! c)  ;; This makes it possible to add only the canvas to the UI-tree and still render stuff correctly
                                        ;; It might be possible to optimize by sending an argument if you only wish to have a canvas or a scrollable canvas
                                        ;; I need to re-familiarize myself on how Swing's scrollpanes repaint themselves
                                        (repaint! scroller)
                                        (.revalidate scroller)

                                        (catch Exception ex
                                          (print-stack-trace ex)))))
    
    {:canvas c
     :scrollable scroller
     }))
