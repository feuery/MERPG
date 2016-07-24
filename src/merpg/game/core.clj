(ns merpg.game.core
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.main-layout :as ml]
            [merpg.reagi :refer :all]
            [reagi.core :as r]
            [seesaw.core :refer :all]
            [clojure.pprint :refer :all]
            [merpg.mutable.registry :refer :all]
            [merpg.game.map-stream :refer [final-image final-img-dimensions]]))

(defn run-game! [& {:keys [hide-editor?
                           fullscreen?
                           editor-frame] :or {hide-editor? true
                                              fullscreen? true
                                              editor-frame ml/f}}]
  (when hide-editor?
    (hide! editor-frame)
    (reset! editor-streams-running? false))

  (register-element! :selected-map (peek-registry :initial-map)) ;; set the correct map and run its scripts which hopefully contain the code to resume the possibly saved game
  
  (reset! game-streams-running? true)

  (def ff (frame :width 800
                :height 600
                :visible? false
                :content (border-panel :center (canvas :paint #(if (realized? final-image)
                                                                 (let [[w h] @final-img-dimensions]
                                                                   (doto %2
                                                                     (.setBackground transparent)
                                                                     (.clearRect 0 0 w h)
                                                                     (.drawImage @final-image 0 0 nil)))
                                                                 (println "Final-image isn't done")))
                                       :south (button :text "Hide!"
                                                       :listen
                                                       [:action (fn [_]
                                                                  (dispose! ff))]))))
  (def tt (timer (fn [_]
                   (invoke-now
                    (repaint! ff)))
                 :start? true
                 :repeats? true
                 :delay 16))
                                                           
  
  (listen ff :window-closed (fn [_]
                              (println "ff closed")
                              (.stop tt)
                              (reset! game-streams-running? false)))
  (full-screen! ff))
