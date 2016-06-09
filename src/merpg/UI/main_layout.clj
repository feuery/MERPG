(ns merpg.UI.main-layout
  (:require [seesaw.core :refer [frame border-panel flow-panel make-widget dispose! config! show!
                                 vertical-panel left-right-split top-bottom-split alert
                                 button menubar menu menu-item]]
            [environ.core :refer [env]]
            [seesaw.bind :as b]
            [seesaw.chooser :refer :all]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.string :as str]
            [merpg.IO.tileset :refer [load-tileset img-to-tileset]]
            [merpg.IO.out :refer [dump-image read-image]]
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
            [merpg.util :refer [vec-remove]]
            [merpg.2D.core :as dd]

            [merpg.mutable.maps :refer [map!]]))

(defn do-resize! [map-atom width height
                  horizontal-anchor
                  vertical-anchor
                  map-renderer]
  (swap! map-atom #(resize % width height
                           :horizontal-anchor horizontal-anchor
                           :vertical-anchor vertical-anchor))
  (.resize_happened map-renderer))

(defn linux? []
  (= (System/getProperty "os.name") "Linux"))

(defn windows? []
  (.contains (System/getProperty "os.name") "Windows"))

(defn get-content [f]  
  (let [current-tool-view (->> "TODO tools are broken"
                               make-widget)]
    (left-right-split
     (vertical-panel
      :items
      [;; (tool-frame! tool-atom current-tool-fn selected-tool)
       "TODO tool-frame is broken"
       "Current tool"
       current-tool-view
       
       (button :text "Resize map"
               :listen
               [:action (fn [_]
                          (alert "TODO Resize is currently broken"))])

       "Current tile"           
       (:canvas (bindable-canvas (atom nil)
                                 (fn [_]
                                   (dd/draw-to-surface (dd/image 100 100 :color "#FFFFFF")
                                                       (dd/Draw "TODO current tile is broken" [0 0])))))
       "Maps"
       (:canvas (bindable-canvas (atom nil)
                                 (fn [_]
                                   (dd/draw-to-surface (dd/image 100 100 :color "#FFFFFF")
                                                       (dd/Draw "TODO map-list is broken" [0 0])))))
       (button :text "Add map"
               :listen
               [:action (fn [_]
                          (alert "TODO map-adding is broken"))])
       
       "Layers"
       (:canvas (bindable-canvas (atom nil)
                                 (fn [_]
                                   (dd/draw-to-surface (dd/image 100 100 :color "#FFFFFF")
                                                       (dd/Draw "TODO layer-list is broken" [0 0])))))
       
       (button :text "New layer"
               :listen
               [:action (fn [_]
                          (alert "TODO new-layer is broken"))])

       (button :text "Remove layer"
               :listen
               [:action (fn [_]
                          (alert "TODO remove-layer is broken"))])
       (button :text "Move up"
               :listen
               [:action (fn [_]
                          (alert "TODO move-up is broken"))])
       (button :text "Move down"
               :listen
               [:action (fn [_]
                          (alert "TODO move-down is broken"))])
       
       "Tilesets"
       (:canvas (bindable-canvas (atom nil)
                                 (fn [_]
                                   (dd/draw-to-surface (dd/image 100 100 :color "#FFFFFF")
                                                       (dd/Draw "TODO tilesets is broken" [0 0])))))
       
       (button :text "Load tileset"
               :listen
               [:action (fn [_]
                          (alert "TODO move-down is broken")
                          (comment
                            (choose-file :filters [["Tilesetit" ["png" "jpg" "jpeg"]]]
                                         :remember-directory? true
                                         :multi? true :success-fn
                                         (fn [_ files]
                                           (let [tilesets (->> files
                                                               (map str)
                                                               (map load-tileset))]
                                             (doseq [ts tilesets]
                                               (swap! tileset-atom assoc (keyword (gensym)) ts)))))))])
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

(defmacro building? [] ;;We do not care about $mbuild on runtime, only when `lein uberjar` is running
  ;; (println "BUILDING-FLAG IS SET TO TRUE")
  ;; (println "INVERT IT OR REPL DIES WITH YOU FRAME")
  `(do false))

(defn show-mapeditor []
  
  (def f (frame :width 800
                :height 600
                :visible? true
                :menubar (make-menu)
                :on-close (if (building?)
                            :exit
                            :hide)
                ))
  (config! f :content (get-content f)))
