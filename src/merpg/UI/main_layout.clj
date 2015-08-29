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
            [merpg.2D.core :as dd]))

(defn do-resize! [map-atom width height
                  horizontal-anchor
                  vertical-anchor]
  (swap! map-atom #(resize % width height
                           :horizontal-anchor horizontal-anchor
                           :vertical-anchor vertical-anchor)))

(defn linux? []
  (= (System/getProperty "os.name") "Linux"))

(defn windows? []
  (.contains (System/getProperty "os.name") "Windows"))

(defn get-content [map-set-atom f tileset-atom current-map-atom current-map-index-atom]
  (def selected-tool (atom :pen))
  (let [map-width  10
        map-height  10
        current-tool-view (->> @selected-tool
                               str
                               make-widget)] ;;The following atoms are needed on the top-level...
    
    (def current-layer-atom (atom nil))  ;; Is set by the layers-listbox

    (println "current-map-atm: " (meta @current-map-atom))
    
    (def current-layer-index-atom (atom 0 :validator (fn [new]
                                                       (println "@current-layer-index-atom validator, new = " new)
                                                       (and (>= new 0)
                                                            (< new (layer-count @current-map-atom)))))) ;;Is set by the layers-listbox                                                       
    
    (def tool-atom (atom {}))
    (def current-tool-fn (atom nil))

    (def current-tileset-index-atom (atom :initial :validator (complement number?)))
    (def current-tileset-atom (atom nil))
    (def current-tile (ref (tile 0 0 :initial 0)))

    (def mouse-down-a? (atom false))
    (def mouse-map-a (atom (make-bool-layer map-width map-height :default-value false))) 
    (add-watch current-map-index-atom :index-watch (fn [_ _ _ new]
                                                     (reset! current-map-atom (get @map-set-atom new))
                                                     (reset! current-layer-index-atom 0)
                                                     (reset! current-layer-atom (get @current-map-atom @current-layer-index-atom)))) ;;Keeps an eye on the Maps - list
    (add-watch current-map-atom :map-watch (fn [_ _ _ new]
                                             (swap! map-set-atom assoc @current-map-index-atom new))) ;; Updates changes to the current map to the global map list

    (b/bind selected-tool (b/transform str) current-tool-view)
  
  (left-right-split
   (vertical-panel
          :items
          [(tool-frame! tool-atom current-tool-fn selected-tool)
           "Current tool"
           current-tool-view
           
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

           ;; (button :text "Relocation functions"
           ;;         :listen
           ;;         [:action (fn [_]
           ;;                    (alert (str "These are set up in the REPL with merpg.mutable.relocation/defn-reloc - macro. The usage is explained in merpg/mutable.relocation.clj (https://github.com/feuery/MERPG/blob/master/src/merpg/mutable/relocation.clj#L26)")))])

           "Current tile"           
           (bindable-canvas current-tile
                            (fn [tile]
                              (-> @tileset-atom
                                  (get (:tileset tile))
                                  (get (:x tile))
                                  (get (:y tile)))))           
           "Maps"
           (bindable-list map-set-atom
                          current-map-atom
                          :custom-model-bind #(-> % meta :name)
                          :selected-index-atom current-map-index-atom
                          :on-select (fn [_]
                                       (property-editor map-set-atom
                                                        :with-meta? true
                                                        :index @current-map-index-atom)))
           (button :text "Add map"
                   :listen
                   [:action (fn [_]
                              (swap! map-set-atom conj (make-map 2 2 2)))])
           
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
           (button :text "New layer"
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
           
           "Tilesets"
           (bindable-list-with-map-source tileset-atom
                          current-tileset-atom
                          :custom-model-bind (fn [[k v]] (str k))
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
                                               (doseq [ts tilesets]
                                                 (swap! tileset-atom assoc (keyword (gensym)) ts))))))])
           (button :text "Remove tileset"
                   :listen
                   [:action (fn [_]
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
                              (reset! current-tileset-index-atom (first (keys @tileset-atom)))))])
           (button :text "Close"
                   :listen
                   [:action (fn [_]
                              (dispose! f))])])
   (top-bottom-split
    (map-controller current-map-atom tool-atom current-tool-fn tileset-atom
                    current-tile current-layer-index-atom selected-tool mouse-down-a? mouse-map-a)
    (tileset-controller tileset-atom
                        current-tileset-index-atom
                        current-tile)
    :divider-location 3/4)
   :divider-location 1/6)))

(defn make-menu [map-list-atom tileset-map-atom current-map-atom current-map-index-atom]
  (menubar :items
           [(menu :text "File"
                  :items
                  [(menu-item :text "Save game image"
                              :listen
                              [:action (fn [_]
                                         (choose-file :filters [["Kartat" ["memap"]]]
                                                      :remember-directory? true
                                                      :type :save
                                                      :multi? false
                                           :success-fn
                                           (fn [_ file]
                                             (dump-image (.getAbsolutePath file) @map-list-atom @tileset-map-atom))))])
                   (menu-item :text "Load game image"
                              :listen
                              [:action (fn [_]
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
                                                           (reset! current-map-atom (get map-set @current-map-index-atom))))))])])]))

(defmacro building? [] ;;We do not care about $mbuild on runtime, only when `lein uberjar` is running
  ;; (println "BUILDING-FLAG IS SET TO TRUE")
  ;; (println "INVERT IT OR REPL DIES WITH YOU FRAME")
  `(do false))

(defn show-mapeditor [map-set-image]
  (println "mapset at show-mapeditor (" (class map-set-image) "): " (meta @map-set-image))

  (def map-set-atom map-set-image)
  (def tileset-atom (atom {:initial (img-to-tileset (dd/image 100 100 :color "#FFFFFF"))}))
  (def current-map-index-atom (atom 0))
  (def current-map-atom (atom (get @map-set-atom @current-map-index-atom)
                              :validator (complement nil?)))
  
  (def f (frame :width 800
                :height 600
                :visible? true
                :menubar (make-menu map-set-atom tileset-atom current-map-atom current-map-index-atom)
                :on-close (if (building?)
                            :exit
                            :hide)
                ))
  (config! f :content (get-content map-set-atom f tileset-atom current-map-atom current-map-index-atom)))
