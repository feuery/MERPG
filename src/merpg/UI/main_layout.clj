(ns merpg.UI.main-layout
  (:require [seesaw.core :refer [frame border-panel flow-panel
                                 vertical-panel left-right-split top-bottom-split alert
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
            [merpg.immutable.map-layer-editing :refer :all]
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
    (def map-set-image (atom [(make-map map-width
                                        map-height
                                        2)] :validator
                                        (fn [new]
                                          (every? #(and (not (nil? (-> % meta :tyyppi (= :map))))
                                                        (-> % meta :tyyppi (= :map))) new))))
    
    (def current-map-index-atom (atom 0))
    (def current-map-atom (atom (get @map-set-image @current-map-index-atom)
                                :validator (complement nil?)))
    
    (def current-layer-atom (atom nil))  ;; Is set by the layers-listbox
    (def current-layer-index-atom (atom 0 :validator (fn [new]
                                                       (println "@current-layer-index-atom validator, new = " new)
                                                       (and (>= new 0)
                                                            (< new (layer-count @current-map-atom)))))) ;;Is set by the layers-listbox                                                       
    
    (def tool-atom (atom {}))
    (def current-tool-fn (atom nil))
    
    (def tileset-atom (atom [(load-tileset "/Users/feuer2/Dropbox/memapper/tileset.png")]))
    (def current-tileset-index-atom (atom 0))
    (def current-tileset-atom (atom nil))
    (def current-tile (ref (tile 0 0 0 0)))

    (add-watch current-map-index-atom :index-watch (fn [_ _ _ new]
                                                     (reset! current-map-atom (get @map-set-image new))
                                                     (reset! current-layer-index-atom 0)
                                                     (reset! current-layer-atom (get @current-map-atom @current-layer-index-atom)))) ;;Keeps an eye on the Maps - list
    (add-watch current-map-atom :map-watch (fn [_ _ _ new]
                                             (swap! map-set-image assoc @current-map-index-atom new))) ;; Updates changes to the current map to the global map list
    )
  
  (left-right-split
   (vertical-panel
          :items
          [(tool-frame! tool-atom current-tool-fn)
           (button :text "Resize map"
                   :listen
                   [:action (fn [_]
                              (let [{ready-state :ready-state :as result-map} (resize-dialog @current-map-atom)]
                                (add-watch ready-state
                                           :resize-watcher
                                           (fn [_ _ _ ready-state]
                                             (println "in resize-thingy")
                                             (when (= ready-state :ok)
                                               (println "Resizing to " [@(:width result-map)
                                                                        @(:height result-map)])
                                               (do-resize! current-map-atom
                                                           @(:width result-map)
                                                           @(:height result-map)
                                                           @(:horizontal-anchor result-map)
                                                           @(:vertical-anchor result-map)))))))])

           (button :text "Relocation functions"
                   :listen
                   [:action (fn [_]
                              (alert (str "These are set up in the REPL with merpg.mutable.relocation/defn-reloc - macro. The usage is explained in merpg/mutable.relocation.clj (https://github.com/feuery/MERPG/blob/master/src/merpg/mutable/relocation.clj#L26)")))])

           "Current tile"           
           (bindable-canvas current-tile
                            (fn [tile]
                              (-> @tileset-atom
                                  (get (:tileset tile))
                                  (get (:x tile))
                                  (get (:y tile)))))           
           "Maps"
           (bindable-list map-set-image
                          current-map-atom
                          :custom-model-bind #(-> % meta :name)
                          :selected-index-atom current-map-index-atom
                          :on-select (fn [_]
                                       (property-editor map-set-image
                                                        :with-meta? true
                                                        :index @current-map-index-atom)))
           (button :text "Add map"
                   :listen
                   [:action (fn [_]
                              (swap! map-set-image conj (make-map 2 2 2)))])
           
           "Layers"
           (bindable-list current-map-atom
                          current-layer-atom
                          :custom-model-bind #(-> % meta :name)
                          :selected-index-atom current-layer-index-atom
                          ;; :reverse? true       
                          :on-select (fn [_]
                                       (property-editor current-map-atom
                                                        :with-meta? true
                                                        :index @current-layer-index-atom)))
           (flow-panel :items
                       [(button :text "New layer"
                                :listen
                                [:action (fn [_]
                                           (swap! current-map-atom conj (make-layer (width @current-map-atom)
                                                                                    (height @current-map-atom))))])

                        (button :text "Remove layer"
                                :listen
                                [:action (fn [_]
                                           (let [old-i @current-layer-index-atom]
                                             (swap! current-layer-index-atom (comp #(if (neg? %) 0 %) dec))
                                             (swap! current-map-atom vec-remove old-i)))])
                        (button :text "Move up"
                                :listen
                                [:action (fn [_]
                                           (when (< (inc @current-layer-index-atom)
                                                    (layer-count @current-map-atom))
                                             (swap! current-map-atom swap-layers @current-layer-index-atom (inc @current-layer-index-atom))
                                             (swap! current-layer-index-atom inc)))])
                        (button :text "Move down"
                                :listen
                                [:action (fn [_]
                                           (when (pos? (dec @current-layer-index-atom))
                                             (swap! current-map-atom swap-layers @current-layer-index-atom (dec @current-layer-index-atom))
                                             (swap! current-layer-index-atom dec)))])
                        ])
           
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
                                               (swap! tileset-atom #(vec (concat % tilesets)))))))])])
   (top-bottom-split
    (map-controller current-map-atom tool-atom current-tool-fn tileset-atom
                    current-tile current-layer-index-atom)
    (tileset-controller tileset-atom
                        current-tileset-index-atom
                        current-tile)
    :divider-location 3/4)
   :divider-location 1/6))
