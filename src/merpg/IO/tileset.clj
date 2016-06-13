(ns merpg.IO.tileset
  (:require [merpg.2D.core :refer :all]
            [clojure.pprint :refer :all]))

(defn img-to-tileset [img]
  (let [partition-limit (long (/ (img-height img) 50))
        [w h] [(img-width img) (img-height img)]
        coordinate-pairs (for [x (range 0 (img-width img) 50)
                               y (range 0 (img-height img) 50)
                               :when (and (<= (+ x 50) w)
                                          (<= (+ y 50) h))]
                           [x y])]
    (->> coordinate-pairs
         (map (fn [[x y]]
                (subimage img x y 50 50)))
         (partition partition-limit)
         (map vec)
         vec)))

(defn load-tileset
  "Path can be either string or bufferedimage"
  [path]
  (if (string? path)
    (img-to-tileset (image path))
    (img-to-tileset path)))

(defn tileset-to-img [tileset]
  (if-not (nil? tileset)
    (try
      (let [width (count tileset)
            height (count (first tileset))
            img-w (* width 50)
            img-h (* 50 height)
            surface (image img-w img-h)]
        (draw-to-surface surface
                         (doseq [x-y (for [x (range 0 img-w 50)
                                           y (range 0 img-h 50)]
                                       [x y])]
                           ;; (println "x-y " x-y)
                           ;; (println "class " (class (get-in tileset (map #(/ % 50) x-y) x-y)))
                           (Draw (get-in tileset (map #(/ % 50) x-y)) x-y))))
      (catch IllegalArgumentException ex
        (pprint tileset)
        (throw ex)))
    (image 50 50)))
