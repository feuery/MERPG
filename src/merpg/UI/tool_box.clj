(ns merpg.UI.tool-box
  (:require [seesaw.core :refer :all]))

;; [map current-tile x y layer]

(defn tool-frame! [tool-collection-atom current-tool-fn-atom]
  (let [f (frame)]
    (defn get-content! [_ _ _ new-tools]
      (config! f :content (vertical-panel :items (vec (map
                                                   (fn [[tool-name tool-fn]]
                                                     (button :text (str tool-name) :listen [:action (fn [_]
                                                                                                (reset!
                                                                                                 current-tool-fn-atom
                                                                                                 tool-fn))]))
                                                   new-tools)))))
    (get-content! nil nil nil @tool-collection-atom)
    (add-watch tool-collection-atom :tool-watcher get-content!)
    (-> f pack! show!)))
