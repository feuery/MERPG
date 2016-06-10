(ns merpg.mutable.tileset-rview
  (:require [reagi.core :as r]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.2D.core :as dd]))

(def rtilesets-watchers (atom {}))
(defn add-rtileset-watcher [f k]
  (swap! rtilesets-watchers assoc k f))

(defn remove-rtileset-watcher [k]
  (swap! rtilesets-watchers dissoc k))

(defn render-tileset! [tileset-id]
  (when-let [{tileset :images} (re/peek-registry tileset-id)]
    (let [w (count tileset)
          h (count (first tileset))]
      (dd/draw-to-surface (dd/image (* 50 w) (* 50 h))
                          (dotimes [x w]
                            (dotimes [y h]
                              (dd/Draw (get-in tileset [x y]) [(* x 50)
                                                            (* y 50)])))))))
(def rendered-tilesets (->> rv/local-registry
                            (r/map (fn [r]
                                     (->> r
                                          (filter #(= (-> % second :type) :tileset))
                                          first
                                          (pmap render-tileset!))))
                            (r/map (fn [r]
                                     (doseq [[_ func] @rtilesets-watchers]
                                       (func))
                                     r))))
