(ns merpg.UI.main-layout
  (:require [seesaw.core :refer [frame border-panel flow-panel make-widget dispose! config! show!
                                 confirm vertical-panel left-right-split top-bottom-split alert
                                 button menubar menu menu-item label]]
            [environ.core :refer [env]]
            [seesaw.bind :as b]
            [clojure.core.async :as a]
            [seesaw.chooser :refer :all]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [merpg.IO.tileset :refer [load-tileset img-to-tileset]]
            [merpg.mutable.tileset :refer [tileset!]]
            [merpg.mutable.tileset-rview :refer [tileset-meta-ui]]
            [merpg.IO.out :refer [dump-image read-image]]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.UI.map-controller :refer [map-controller
                                             show]]
            [merpg.UI.tileset-controller :refer :all]
            [merpg.UI.tool-box :refer [tool-frame!]]
            [merpg.UI.BindableCanvas :refer :all]
            [merpg.UI.BindableList :refer :all]
            [merpg.UI.property-editor :refer :all]
            [merpg.UI.dialogs.resize-dialog :refer [resize-dialog]]
            [merpg.UI.current-tile-view :refer :all]
            ;; [merpg.immutable.basic-map-stuff :refer :all]
            ;; [merpg.immutable.map-layer-editing :refer :all]
            [merpg.util :refer [vec-remove]]
            [merpg.2D.core :as dd]
            [merpg.mutable.tools :as tools]
            [merpg.mutable.maps :refer [map! map-metas-ui
                                        mapwidth! mapheight!]]
            [merpg.mutable.layers :refer [layer-metas-ui layer!
                                          layer-count!]]
            [merpg.mutable.registry :as re]
            [merpg.mutable.to-registry-binding :as trb]))

(defn linux? []
  (= (System/getProperty "os.name") "Linux"))

(defn windows? []
  (.contains (System/getProperty "os.name") "Windows"))

(defn tool-collection-to-buttons [tool-col]
  (->> tool-col
       (mapv (fn [s]
               (button :text (str s)
                       :listen [:action (fn [_]
                                          (re/register-element! :selected-tool
                                                                s))])))))

(defn get-content [f]  
  (let [current-tool-view (label :text (str @tools/selected-tool-ui))
        all-tools-view (vertical-panel :items (tool-collection-to-buttons @tools/all-tools-ui))]
    (b/bind tools/selected-tool-ui
            (b/transform str)
            (b/property current-tool-view :text))
    (b/bind tools/all-tools-ui
            (b/transform tool-collection-to-buttons)
            (b/property all-tools-view :items))
    (left-right-split
     (vertical-panel
      :items
      [all-tools-view
       "Current tool"
       current-tool-view
       
       (button :text "Resize map"
               :listen
               [:action (fn [_]
                          (alert "TODO Resize is currently broken"))])

       "Current tile"           
       (current-tile-view)
       
       "Maps"
       (atom-to-jlist map-metas-ui :key :selected-map)
        
       (button :text "Add map"
               :listen
               [:action (fn [_]
                          (let [viewmodel (atom {"Map's name" ""
                                           "Map's width" 0
                                                 "Map's height" 0
                                                 "Amount of layers" 0})
                                c (ask-box viewmodel)]
                            (a/go
                              (let [result (a/<! c)]
                                (if result
                                  (let[{name "Map's name"
                                     w "Map's width"
                                     h "Map's height"
                                     l "Amount of layers"} @viewmodel]
                                (when (->> [w h l]
                                           (every? #(> % 0)))
                                  (println "Creating map with params " [w h l])
                                  (println "map created with id " (map! w h l)))))))))])

       (button :text "Remove map"
               :listen
               [:action (fn [_]
                          (let [selected-map (-> :selected-map
                                                 re/peek-registry ;; id
                                                 re/peek-registry)] ;;data
                            (when (confirm (str "You're about to IRRECOVERABLY delete map with a name " (:name selected-map) "\n\nDo you wish to proceed?"))
                              (re/remove-element! (re/peek-registry :selected-map))
                              (let [new-map-id (-> @map-metas-ui
                                                   first
                                                   first)]
                                (re/register-element! :selected-map new-map-id)))))])
                                
       
       "Layers"
       (atom-to-jlist layer-metas-ui :key :selected-layer)
       
       (button :text "New layer"
               :listen
               [:action (fn [_]
                          (let [smap (re/peek-registry :selected-map)]
                            (layer! (mapwidth! smap)
                                    (mapheight! smap)
                                    :parent-id smap
                                    :order (inc (layer-count! smap)))))])

       (button :text "Remove layer"
               :listen
               [:action (fn [_]
                          (let [selected-layer (-> :selected-layer
                                                   re/peek-registry
                                                   re/peek-registry)]
                            (when (confirm (str "You're about to IRRECOVERABLY delete layer with a name " (:name selected-layer) "\n\nDo you wish to proceed?"))
                              (re/remove-element! (re/peek-registry :selected-layer))
                              (let [new-layer-id (-> @layer-metas-ui
                                                     first
                                                     first)]
                                (re/register-element! :selected-layer new-layer-id)))))])
       (button :text "Move up"
               :listen
               [:action (fn [_]
                          (alert "TODO move-up is broken"))])
       (button :text "Move down"
               :listen
               [:action (fn [_]
                          (alert "TODO move-down is broken"))])
       
       "Tilesets"
       (atom-to-jlist tileset-meta-ui :key :selected-tileset)
       
       (button :text "Load tileset"
               :listen
               [:action (fn [_]
                          (choose-file :filters [["Tilesetit" ["png" "jpg" "jpeg"]]]
                                         :remember-directory? true
                                         :multi? true :success-fn
                                         (fn [_ files]
                                           (let [tilesets (->> files
                                                               (map str)
                                                               (mapv tileset!))]
                                             (println "Loaded tilesets!")))))])
       (button :text "Remove tileset"
               :listen
               [:action (fn [_]
                          (alert "TODO move-down is broken")                              
                          (comment
                            (dosync
                             (let [m @current-map-atom
                                   coordinates (for [l (range (layer-count m))
                                                     x (range (width m))
                                                     y (range (height m))
                                                     :when (= (:tileset (get-tile m l x y)) @current-tileset-index-atom)]
                                                 [l x y])]
                               (doseq [[l x y] coordinates]
                                 (swap! current-map-atom
                                        set-tile
                                        l x y
                                        (tile 0 0 :initial 0))))
                             (ref-set current-tile (tile 0 0 :initial 0))
                             (swap! tileset-atom dissoc @current-tileset-index-atom)
                             (reset! current-tileset-index-atom (first (keys @tileset-atom))))))])
       (button :text "Close"
               :listen
               [:action (fn [_]
                          (dispose! f))])])
     (top-bottom-split
      (map-controller )
      (tileset-controller)
      :divider-location 3/4)
     :divider-location 1/6)))

(defn make-menu []
  (menubar :items
           [(menu :text "File"
                  :items
                  [(menu-item :text "Save game image"
                              :listen
                              [:action (fn [_]
                                         (comment
                                           (choose-file :filters [["Kartat" ["memap"]]]
                                                        :remember-directory? true
                                                        :type :save
                                                        :multi? false
                                                        :success-fn
                                                        (fn [_ file]
                                                          (dump-image (.getAbsolutePath file) @map-list-atom @tileset-map-atom))))
                                         (alert "TODO broken"))])
                   (menu-item :text "Load game image"
                              :listen
                              [:action (fn [_]
                                         (comment
                                           (choose-file :filters [["Kartat" ["memap"]]]
                                                        :remember-directory? true
                                                        :multi? false
                                                        :success-fn
                                                        (fn [_ file]
                                                          (let [{map-set :maps
                                                                 tilesets :tilesets} (read-image (.getAbsolutePath file))]
                                                            (reset! map-list-atom map-set)
                                                            (reset! tileset-map-atom tilesets)
                                                            (reset! current-map-index-atom 0)
                                                            (reset! current-map-atom (get map-set @current-map-index-atom))))))
                                         (alert "TODO broken"))])])]))

(defn show-mapeditor []
  
  (def f (frame :width 800
                :height 600
                :visible? true
                :menubar (make-menu)
                :on-close 
                            ;; :exit
                :hide))
  (config! f :content (get-content f)))
