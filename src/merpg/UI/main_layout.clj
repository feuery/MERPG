(ns merpg.UI.main-layout
  (:require [seesaw.core :refer :all]
            [merpg.IO.tileset :refer [load-tileset]]
            [merpg.UI.map-controller :refer [map-controller]]
            [merpg.UI.tileset-controller :refer :all]
            [merpg.UI.tool-box :refer [tool-frame!]]
            [merpg.immutable.basic-map-stuff :refer [make-map tile]]))

(defn get-content []
  (let [map-width  10
        map-height  10] ;;The following atoms are needed on the top-level...
    (def map-data-image (ref (make-map map-width
                                       map-height
                                       2)))
    (def tool-atom (atom {}))
    (def current-tool-fn (atom nil))
    
    (def tileset-ref (ref []));; (load-tileset "/Users/feuer2/Dropbox/memapper/tileset.png")]))
    (def current-tileset-index (ref 0))

    (def current-tile (ref (tile 0 0 0 0))))
  
  (border-panel
   :center
   (top-bottom-split
    (map-controller map-data-image tool-atom current-tool-fn tileset-ref)
    (tileset-controller tileset-ref
                        current-tileset-index
                        current-tile)
    :divider-location 3/4)
   :west (vertical-panel :items [(tool-frame! tool-atom current-tool-fn)])))
