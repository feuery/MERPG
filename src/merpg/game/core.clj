(ns merpg.game.core
  (:require [merpg.2D.core :refer :all]
            [merpg.UI.main-layout :as ml]
            [merpg.reagi :refer :all]
            [reagi.core :as r]
            [seesaw.core :refer :all]
            [clojure.pprint :refer :all]
            [merpg.mutable.registry :refer :all]
            [merpg.game.map-stream :refer [final-image final-img-dimensions]]
            [merpg.game.keyboard :refer [keycodes-down]])
  (:import [java.awt.event KeyEvent]))



;; (def keydown-stream
;;   "The keycodes game-frame receives will be sent to this stream. Key-still-down events are too delivered here, as per JFrame's keylistener functionality. If you need to explicitly know when key isn't down anymore, subscribe to the keyup-stream."
;;   (game-stream (r/events)))

;; (def keyup-stream
;;   "The keycodes game-frame receives will be sent to this stream"
;;   (game-stream (r/events)))
               ;; (r/map (fn [k]
               ;;          (swap! keycodes-down disj k)
               ;;          k))))

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

  (def ff (doto (frame :width 800
                       :height 600
                       :visible? false
                       :listen
                       [:key-released (fn [e]
                                        (swap! keycodes-down disj (.getKeyCode e)))
                        :key-pressed (fn [e]
                                       (swap! keycodes-down conj (.getKeyCode e)))]
                       :content (border-panel :center (canvas :paint #(if (realized? final-image)
                                                                        (let [[w h] @final-img-dimensions]
                                                                          (doto %2
                                                                            (.setBackground transparent)
                                                                            (.clearRect 0 0 w h)
                                                                            (.drawImage @final-image 0 0 nil)))))
                                              :south (button :text "Hide!"
                                                             :listen
                                                             [:action (fn [_]
                                                                        (dispose! ff))])))
            (.setFocusable true)
            (.setFocusTraversalKeysEnabled false)))
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
