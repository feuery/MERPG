(ns merpg.UI.tool-box
  (:require [seesaw.core :refer :all]))
            

;; [map current-tile x y layer]

(defn tool-frame! [tool-collection-atom current-tool-fn-atom selected-tool]
  (let [v (vertical-panel)]
    (defn get-content! [_ _ _ new-tools]
      (config! v :items (vec
                         (cons "Tools"
                                 (map
                                  (fn [[tool-name tool-fn]]
                                    (button :text (str tool-name) :listen [:action (fn [_]
                                                                                     (reset!
                                                                                      current-tool-fn-atom
                                                                                      tool-fn)
                                                                                     (reset! selected-tool tool-name)
                                                                                     )]))
                                  new-tools)))))
    (get-content! nil nil nil @tool-collection-atom)
    (add-watch tool-collection-atom :tool-watcher get-content!)
    v))
