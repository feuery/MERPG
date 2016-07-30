(ns merpg.game.core
  (:require [merpg.2D.core :refer :all]
            [merpg.reagi :refer :all]
            [reagi.core :as r]
            [seesaw.core :refer :all]
            [clojure.pprint :refer :all]
            [merpg.mutable.registry :refer :all]
            [merpg.mutable.maps :refer [load-map-scripts!]]
            [merpg.game.map-stream :refer [final-image]]
            [merpg.game.keyboard :refer [keycodes-down]])
  (:import [java.awt.event KeyEvent]))

(defn run-game! [& {:keys [hide-editor?
                           fullscreen?
                           editor-frame
                           on-close] :or {hide-editor? true
                                          fullscreen? true
                                          editor-frame nil
                                          on-close identity}}]
  (when hide-editor?
    (if (some? editor-frame)
      (hide! editor-frame))
    (reset! editor-streams-running? false))

  (register-element! :selected-map (peek-registry :initial-map))
  (load-map-scripts! (peek-registry :selected-map))
  
  (reset! game-streams-running? true)
  
  (let [c (canvas :paint #(doto %2
                            (.drawImage @final-image 0 0 nil)))]
    (def ff (doto (frame :width 800
                         :height 600
                         :visible? false
                         :listen
                         [:key-released (fn [e]
                                          (swap! keycodes-down disj (.getKeyCode e)))
                          :key-pressed (fn [e]
                                         (swap! keycodes-down conj (.getKeyCode e)))]
                         :content (border-panel :center c
                                                :south (grid-panel :columns 2
                                                                   :items
                                                                   [(button :text "Close game"
                                                                            :listen
                                                                            [:action (fn [_]
                                                                                       (dispose! ff))])

                                                                    (button :text "Open editor"
                                                                            :listen
                                                                            [:action (fn [_]
                                                                                       (if (some? editor-frame)
                                                                                         (do
                                                                                           (reset! editor-streams-running? true)
                                                                                           (show! editor-frame))
                                                                                         (alert "Editor frame isn't initialized")))])])))
              (.setFocusable true)
              (.setFocusTraversalKeysEnabled false)))
    (def tt (timer (fn [_]
                     (invoke-now
                      (repaint! c)))
                   :start? true
                   :repeats? true
                   :delay 16))
    
    
    (listen ff :window-closed (fn [_]
                                (println "ff closed")
                                (.stop tt)
                                (reset! game-streams-running? false)
                                (on-close :nil)
                                (if (or (nil? editor-frame)
                                        (not (visible? editor-frame)))
                                  (System/exit 0))))
    (full-screen! ff)))
