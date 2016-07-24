(ns merpg.game.map-stream
  (:require [merpg.2D.core :refer :all]
            [merpg.reagi :refer :all]
            [merpg.mutable.registry :refer :all]
            [merpg.mutable.registry-views :as rv]
            [merpg.UI.render-drawqueue :refer :all]

            [reagi.core :as r]
            [seesaw.core :refer :all]
            [clojure.pprint :refer :all]))

(def rendered-map
  (game-stream
   (r/sample 1000 registry)
   (r/map #(rv/registry->rendered-map (peek-registry :selected-map) %))))

(def final-image (game-stream
                  (r/sample 16 (r/zip rendered-map drawqueue))
                  (r/map (fn [r]
                           (reduce (fn [map-surface drawqueue]
                                     (draw-to-surface map-surface
                                                      (Draw drawqueue [0 0]))) r)))))

(def final-img-dimensions (->> final-image
                               (r/map (fn [img]
                                        [(img-width img) (img-height img)]))))


