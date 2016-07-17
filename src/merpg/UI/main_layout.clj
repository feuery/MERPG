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
                                           vertical-anchor))))))])
                                  
                                         

       "Current tile"           
       (current-tile-view)

       "Document Tree"
       (domtree)

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
                                                          (read-image! (.getAbsolutePath file)))))])])]))

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
