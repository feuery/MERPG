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
            [merpg.UI.BindableList :refer :all]
            [merpg.UI.current-tile-view :refer :all]
            [merpg.UI.tree :refer [domtree]]
            [merpg.util :refer [vec-remove]]
            [merpg.2D.core :as dd]
            [merpg.mutable.tools :as tools]
            [merpg.mutable.maps :refer [map! map-metas-ui]]
            [merpg.mutable.layers :as l :refer [layer-metas-ui layer!
                                                mapwidth! mapheight!
                                                layer-count!]]
            [merpg.mutable.registry :as re]
            [merpg.mutable.to-registry-binding :as trb]
            [merpg.mutable.resize-algorithms :refer [resize!]]))

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
                          (let [w (->> :selected-map
                                       re/peek-registry
                                       mapwidth!)
                                h (->> :selected-map
                                       re/peek-registry
                                       mapheight!)
                                vm (atom {"Map's width" w
                                          "Map's height" h
                                          "Side of horizontal action" [:left :right]
                                          "Side of vertical action" [:top :bottom]})
                                c (ask-box vm)]
                            (a/go
                              (when (a/<! c)
                                (let [{w "Map's width"
                                       h "Map's height"
                                       horizontal-anchor "Side of horizontal action"
                                       vertical-anchor "Side of vertical action"} @vm]
                                  (resize! (re/peek-registry :selected-map)
                                           w
                                           h
                                           horizontal-anchor
                                           vertical-anchor))))))])
                                  
                                         

       "Current tile"           
       (current-tile-view)

       "Document Tree"
       (domtree)
       
       "Maps"        
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
                                  (println "map created with id " (map! w h l :name name)))))))))])

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
                          (let [selected-layer (re/peek-registry :selected-layer)
                                selected-order (-> selected-layer
                                                   re/peek-registry
                                                   :order)
                                orders (->> @l/layer-metas-ui
                                            (map second)
                                            (map :order))
                                max-ord (apply max orders)]
                            (when (<= (inc selected-order) max-ord)
                              (when (< (inc selected-order) max-ord)
                                (if-let [upper-id (->> @l/layer-metas-ui
                                                    (filter #(-> % second :order (= (inc selected-order))))
                                                    (map first)
                                                    first)]
                                  ;; lower the upper
                                  (re/update-registry upper-id
                                                      (update upper-id :order dec))))
                              ;; raise the lower
                              
                              (re/update-registry selected-layer
                                                  (update selected-layer :order inc)))))])
       (button :text "Move down"
               :listen
               [:action (fn [_]
                          (let [selected-layer (re/peek-registry :selected-layer)
                                selected-order (-> selected-layer
                                                   re/peek-registry
                                                   :order)]
                            (when (>= (dec selected-order) 0)
                              (when (> (dec selected-order) 0)
                                (if-let [lower-id (->> @l/layer-metas-ui
                                                       (filter #(-> % second :order (= (dec selected-order))))
                                                       (map first)
                                                       first)]
                                  (re/update-registry lower-id
                                                      (update lower-id :order inc))))
                              (re/update-registry selected-layer
                                                  (update selected-layer :order dec)))))])
       
       "Tilesets"       
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
                          (let [selected-tileset (re/peek-registry :selected-tileset)]
                            (if-not (= selected-tileset :initial)
                              (let [{:keys [name]} (re/peek-registry selected-tileset)
                                    tiles-to-update (->> @re/registry
                                                         (filter (fn [[_ val]]
                                                                   (and
                                                                    (= (:type val) :tile)
                                                                    (= (:tileset val) selected-tileset))))
                                                         (map first))]
                                (when (confirm (str "You're about to IRRECOVERABLY delete tileset with a name " name "\n\nDo you wish to proceed?"))
                                  (re/remove-element! selected-tileset)
                                  (doseq [tile-id tiles-to-update]
                                    (re/update-registry tile-id
                                                        (assoc tile-id
                                                               :x 0
                                                               :y 0
                                                               :tileset :initial)))))
                              (alert "You can't remove tileset :initial"))))])
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
