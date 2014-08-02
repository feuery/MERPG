(ns merpg.UI.main-layout
  (:require [seesaw.core :refer [frame border-panel
                                 vertical-panel top-bottom-split
                                 button]]
            [seesaw.chooser :refer :all]
            [clojure.stacktrace :refer [print-stack-trace]]
            [merpg.IO.tileset :refer [load-tileset]]
            [merpg.UI.map-controller :refer [map-controller
                                             show]]
            [merpg.UI.tileset-controller :refer :all]
            [merpg.UI.tool-box :refer [tool-frame!]]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.UI.BindableList :refer :all]
            [merpg.UI.property-editor :refer :all]
            [merpg.UI.dialogs.resize-dialog :refer [resize-dialog]]
            [merpg.immutable.basic-map-stuff :refer :all]
            [merpg.util :refer [vec-remove]]))

(defn do-resize! [map-atom width height
                  horizontal-anchor
                  vertical-anchor]
  (swap! map-atom #(resize % width height
         :horizontal-anchor horizontal-anchor
         :vertical-anchor vertical-anchor)))

(defn get-content []
  (let [map-width  10
        map-height  10] ;;The following atoms are needed on the top-level...
    (def map-data-image (atom (make-map map-width
                                       map-height
                                       2)
                              :validator
                              (fn [new]
                                (println "@map-data-image validator, new: " (-> new meta :tyyppi))
                                (and (not (nil? (-> new meta :tyyppi (= :map))))
                                     (-> new meta :tyyppi (= :map))))))
    (def current-layer-atom (atom nil))
    (def current-layer-index-atom (atom 0 :validator (fn [new]
                                                       (println "@current-layer-index-atom validator, new = " new)
                                                       (and (>= new 0)
                                                            (< new (layer-count @map-data-image))))))                                                       
    
    (def tool-atom (atom {}))
    (def current-tool-fn (atom nil))
    
    (def tileset-atom (atom [(load-tileset "/Users/feuer2/Dropbox/memapper/tileset.png")]))
    (def current-tileset-index-atom (atom 0))
    (def current-tileset-atom (atom nil))
    (def current-tile (ref (tile 0 0 0 0))))
  
  (border-panel
   :center
   (top-bottom-split
    (map-controller map-data-image tool-atom current-tool-fn tileset-atom
                    current-tile current-layer-index-atom)
    (tileset-controller tileset-atom
                        current-tileset-index-atom
                        current-tile)
    :divider-location 3/4)
   :west (vertical-panel
          :items
          [(tool-frame! tool-atom current-tool-fn)
           (button :text "Resize map"
                   :listen
                   [:action (fn [_]
                              (let [{ready-state :ready-state :as result-map} (resize-dialog @map-data-image)]
                                (add-watch ready-state
                                           :resize-watcher
                                           (fn [_ _ _ ready-state]
                                             (println "in resize-thingy")
                                             (when (= ready-state :ok)
                                               (println "Resizing to " [@(:width result-map)
                                                                        @(:height result-map)])
                                               (do-resize! map-data-image
                                                           @(:width result-map)
                                                           @(:height result-map)
                                                           @(:horizontal-anchor result-map)
                                                           @(:vertical-anchor result-map)))))))])
           
           (bindable-canvas current-tile
                            (fn [tile]
                              (-> @tileset-atom
                                  (get (:tileset tile))
                                  (get (:x tile))
                                  (get (:y tile)))))           
           
           "Layers"
           (bindable-list map-data-image
                          current-layer-atom
                          :custom-model-bind #(-> % meta :name)
                          :selected-index-atom current-layer-index-atom
                          ;; :reverse? true       
                          :on-select (fn [_]
                                       (property-editor map-data-image
                                                        :with-meta? true
                                                        :index @current-layer-index-atom)))
           (button :text "New layer"
                   :listen
                   [:action (fn [_]
                              (swap! map-data-image conj (make-layer (width @map-data-image)
                                                                     (height @map-data-image))))])

           (button :text "Remove layer"
                   :listen
                   [:action (fn [_]
                              (let [old-i @current-layer-index-atom]
                                (swap! current-layer-index-atom (comp #(if (neg? %) 0 %) dec))
                                (swap! map-data-image vec-remove old-i)))])
           
           "Tilesets"
           (bindable-list tileset-atom
                          current-tileset-atom
                          :custom-model-bind (fn [_]
                                               (str (inc @current-tileset-index-atom) "th"))
                          :selected-index-atom current-tileset-index-atom
                          :on-select (fn [_]
                                       (property-editor tileset-atom
                                                        :with-meta? true
                                                        :index @current-tileset-index-atom)))
           (button :text "Load tileset"
                   :listen
                   [:action (fn [_]
                              (choose-file :filters [["Tilesetit" ["png" "jpg" "jpeg"]]]
                                           :remember-directory? true
                                           :multi? true :success-fn
                                           (fn [_ files]
                                             (let [tilesets (->> files
                                                                 (map str)
                                                                 (map load-tileset))]
                                               (swap! tileset-atom #(vec (concat % tilesets)))))))])
                                                  
                                                  ])))
