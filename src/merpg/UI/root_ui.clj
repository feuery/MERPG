(ns merpg.UI.root-ui
  (:require [merpg.UI.tree :refer [popupmenu]]
            [merpg.UI.askbox :refer [ask-box]]
            [merpg.mutable.registry :as re]
            [merpg.mutable.to-registry-binding :as tbr]
            [seesaw.core :refer :all]
            [seesaw.chooser :refer :all]
            [clojure.core.async :as a]
            [merpg.mutable.maps :refer [map! map-metas-ui]]
            [merpg.mutable.tileset :refer [tileset!]]
            [merpg.mutable.layers :refer [layer! mapwidth! mapheight! layer-count!]]))

(defmethod popupmenu ":root" [_]
  (popup :items [(menu-item :text "Add Map"
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
                 (menu-item :text "Load tileset"
                            :listen
                            [:action (fn [_]
                                       (choose-file :filters [["Tilesets" ["png" "jpg" "jpeg"]]]
                                                    :remember-directory? true
                                                    :all-files? false
                                                    :multi? true :success-fn
                                                    (fn [_ files]
                                                      (let [tilesets (->> files
                                                                          (map str)
                                                                          (mapv tileset!))]
                                                        (println "Loaded tilesets!")))))])]))
                                                        
