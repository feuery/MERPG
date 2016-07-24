(ns merpg.mutable.tileset-rview
  (:require [reagi.core :as r]
            [clojure.core.async :as a]
            [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [merpg.mutable.tools :as t]
            [merpg.reagi :refer [editor-stream]]
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
      [tileset-id (dd/draw-to-surface (dd/image (* 50 w) (* 50 h))
                                      (dotimes [x w]
                                        (dotimes [y h]
                                          (dd/Draw (get-in tileset [x y]) [(* x 50)
                                                                           (* y 50)]))))])))
(def rendered-tilesets (editor-stream (r/sample 600 re/registry)
                                      (r/map (fn [r]
                                               (->> r
                                                    (filter #(= (-> % second :type) :tileset))
                                                    (map first)
                                                    (pmap render-tileset!)
                                                    (into {}))))
                                      (r/map (fn [r]
                                               (doseq [[_ func] @rtilesets-watchers]
                                                 (func))
                                               r))))

(t/make-atom-binding tileset-meta {:allow-seq? true}
                     (editor-stream (r/sample 1000 re/registry)
                                    (r/map (fn [r]
                                             (->> r
                                                  (filterv #(= (-> % second :type) :tileset)))))))


(def selected-tileset-view (editor-stream (r/sample 1000 re/registry)
                                          (r/filter #(and (coll? %)
                                                          (contains? % :selected-tileset)))
                                          (r/map :selected-tileset)))

(def c (a/chan))
(def selected-tileset-ui (atom :nil :validator (complement coll?)))

(r/subscribe selected-tileset-view c)

(a/go-loop [data (a/<! c)]
  (when (some? data)
    (reset! selected-tileset-ui data)
    (recur (a/<! c))))
