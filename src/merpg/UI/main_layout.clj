(ns merpg.UI.main-layout
  (:require [seesaw.core :refer :all]
            [seesaw.mig :refer :all]
            [environ.core :refer [env]]
            [seesaw.bind :as b]
            [clojure.core.async :as a]
            [seesaw.chooser :refer :all]
            [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [merpg.IO.tileset :refer [load-tileset img-to-tileset]]
            [merpg.mutable.tileset :refer [tileset!]]
            [merpg.mutable.tileset-rview :refer [tileset-meta-ui] :as tr]
            [merpg.IO.out :refer [dump-image read-image!]]
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
            [merpg.UI.layers-ui]
            [merpg.UI.root-ui]
            [merpg.UI.tileset-ui]
            [merpg.UI.maps-ui]
            [merpg.UI.spritesheet-popupmenu]
            [merpg.UI.scripts-ui]
            [merpg.mutable.registry :as re]
            [merpg.mutable.to-registry-binding :as trb]
            [merpg.mutable.resize-algorithms :refer [resize!]]
            [merpg.settings.core :refer [get-prop! set-prop!] :as settings]
            [merpg.reagi :refer :all]
            [merpg.game.core :refer [run-game!]])
  (:import [java.awt Component]))

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

(extend-type clojure.lang.Atom
  seesaw.make-widget/MakeWidget
  (make-widget* [v]
    (let [val @v]
      (if-not (instance? java.lang.Boolean val)
        (throw (Exception. "Atom-makewidget binding supports currently only atoms with boolean values")))
      (let [widget (checkbox :selected? val)]
        (b/bind (b/selection widget)
                v)
        (b/bind v
                (b/selection widget))
        widget))))

(re/register-element! :preserve-registry true)
      

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
     (mig-panel
      :constraints ["" "[]" "[]"]
      :items
      [[(config! (make-widget editor-streams-running?) :text "Rendering running") "wrap"]
       [(config! (make-widget game-streams-running?) :text "Game rendering") "wrap"]
       [all-tools-view "wrap"]
       ["Current tool" "wrap"]
       [current-tool-view "wrap"]
       
       [(button :text "Resize map"
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
                                          "Side of vertical action" [:top :bottom]
                                          :meta
                                          {"Map's width"
                                           {:max 20
                                            :min 0}
                                           "Map's height"
                                           {:max 20
                                            :min 0}}})
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
                                           vertical-anchor))))))]) "wrap"]
       ["" "wrap"]
       [(vertical-panel :items
                    [(config! (make-widget (trb/atom->registry-binding :preserve-registry)) :text "Preserve registry while playing")
                     (button :text "Run game"
                             :listen [:action (fn [_]
                                                (let [old-registry @re/registry]
                                                  (run-game! :hide-editor? false
                                                             :on-close (fn []
                                                                         (when (re/peek-registry :preserve-registry)
                                                                           (reset! re/registry old-registry))))))])]) "wrap"]
       
       ["Current tile" "wrap"]           
       [(current-tile-view) "span"]

       ["Document Tree" "wrap"]
       [(domtree) "wrap"]

       [(button :text "Close"
               :listen
               [:action (fn [_]
                          (dispose! f))]) "wrap"]])
     (top-bottom-split
      (map-controller )
      (tileset-controller)
      :divider-location 3/4)
     :divider-location 1/6)))

(defn make-menu []
  (let [menu (menubar :items
                      [(menu :text "File"
                             :items
                             [(menu-item :text "Save game image"
                                         :listen
                                         [:action (fn [_]
                                                    (choose-file :filters [["Kartat" ["memap"]]]
                                                                 :remember-directory? true
                                                                 :all-files? false
                                                                 :type :save
                                                                 :multi? false
                                                                 :success-fn 
                                                                 (fn [_ file]
                                                                   (dump-image (.getAbsolutePath file) @re/registry @tr/rendered-tilesets))))])
                              (menu-item :text "Load game image"
                                         :listen
                                         [:action (fn [_]
                                                    (choose-file :filters [["Kartat" ["memap"]]]
                                                                 :all-files? false
                                                                 :remember-directory? true
                                                                 :multi? false
                                                                 :success-fn
                                                                 (fn [_ file]
                                                                   (if (read-image! (.getAbsolutePath file))
                                                                     (println (.getAbsolutePath file) " loaded")
                                                                     (println "Loading " (.getAbsolutePath file) " failed.")))))])

                              
                              (checkbox-menu-item :text "nREPL running?"
                                                  :id :nrepl-menu
                                                  :selected? (get-prop! :nrepl-running?)
                                                  :listen [:action (fn [_]
                                                                     (let [running? (not (get-prop! :nrepl-running?))]
                                                                       (println "Running? " running?)
                                                                       (set-prop! :nrepl-running? running?)))])
                              (menu-item :text "Settings"
                                         :listen [:action (fn [_]
                                                            (ask-box settings/settings :settings-vm? true))])])])]
    (settings/add-prop-watch :nrepl-running? :menu-updater (fn [running?]
                                                             (config! (select menu [:#nrepl-menu]) :selected? running?)))
    menu))
    

(defn show-mapeditor []
  
  (def f (frame :width 800
                :height 600
                :visible? true
                :menubar (make-menu)
                :on-close 
                ;; :exit
                :hide
                ))
  (config! f :content (get-content f)))

 ;; (config! f :menubar (make-menu))
