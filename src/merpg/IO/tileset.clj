(ns merpg.IO.tileset
  (:require [merpg.2D.core :refer :all]))

(defn load-tileset [path]
  (let [img (image path)
        partition-limit (/ (img-height img) 50)
        coordinate-pairs (for [x (range 0 (img-width img) 50)
                               y (range 0 (img-height img) 50)]
                           [x y])]
    (->> coordinate-pairs
         (map (fn [[x y]]
           (subimage img x y 50 50)))
         (partition partition-limit)
         (map vec)
         vec)))

(defn tileset-to-img [tileset]
  (if-not (nil? tileset)
    (let [width (count tileset)
          height (count (first tileset))
          img-w (* width 50)
          img-h (* 50 height)
          surface (image img-w img-h)]
      (draw-to-surface surface
                       (doseq [x-y (for [x (range 0 img-w 50)
                                         y (range 0 img-h 50)]
                                     [x y])]
                         (println "x-y " x-y)
                         (println "class " (class (get-in tileset (map #(/ % 50) x-y) x-y)))
                         (Draw (get-in tileset (map #(/ % 50) x-y)) x-y))))
    (image 50 50)))
