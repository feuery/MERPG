(ns merpg.vfs.icon
  (:require [seesaw.core :refer :all :rename {icon seesaw-icon}]
            [merpg.2D.core :refer :all]))

(defn make-icon [child dir? name & {:keys [on-click]
                                      :or {on-click (fn [subnode] subnode)}}]
  (let [toret (canvas :paint (fn [_ g]
                               (.drawImage g (draw-to-surface (image 100 100)
                                                              (with-color (if dir? "#FFCC33" "#99FF66")
                                                                (Rect 0 0 100 100 :fill? true))
                                                              (with-color "#000000"
                                                                (Draw (str name) [0 30]))) 0 0 nil))
                      :size [100 :by 100])]
    (listen toret :mouse-clicked (fn [_]
                                   (on-click child)))
    toret))
