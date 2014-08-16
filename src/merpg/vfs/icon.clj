(ns merpg.vfs.icon
  (:require [seesaw.core :refer :all :rename {icon seesaw-icon}]
            [merpg.vfs :refer [is-directory]]
            [merpg.2D.core :refer :all]))

(defn get-child [cwd child-id]
  (->> cwd
       (filter (fn [child2]
                 (-> child2
                     meta
                     :node-id
                     (= child-id))))
       first))

(defn make-icon [cwd-atm child-id & {:keys [on-click title-transform]
                                      :or {on-click (fn [subnode] subnode)
                                           title-transform (fn [_] "title-transform not really overridden!")}}]
  (let [toret (canvas :paint (fn [_ g]
                               (.drawImage g
                                          (let [child (get-child @cwd-atm child-id)
                                                dir? (is-directory child)]
                                            (if-not (nil? child)
                                              (draw-to-surface (image 100 100)
                                                               (with-color (if dir? "#FFCC33" "#99FF66")
                                                                 (Rect 0 0 100 100 :fill? true))                          
                                                               (with-color "#000000"
                                                                 (Draw (str (title-transform child)) [0 30])))
                                              (draw-to-surface (image 100 100)                                                               
                                                               (with-color "#000000"
                                                                 (Draw (str "Child " child-id " is not found") [0 0])))))
                                          0 0 nil))
                      :size [100 :by 100])]
    (listen toret :mouse-clicked (fn [_]
                                   (on-click (get-child @cwd-atm child-id))))
    (add-watch cwd-atm :icon-repainter (fn [_ _ _ _]
                                         (repaint! toret)))
    toret))
