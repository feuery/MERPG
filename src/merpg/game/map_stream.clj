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
                  (r/sample 16 r/time)
                  (r/map (fn [_]
                           (let [m @rendered-map
                                 [w h] [(img-width m)
                                        (img-height m)]]
                             (draw-to-surface (image w h)
                                              (Draw m [0 0])
                                              (Draw (render-drawqueue!) [0 0])))))))

