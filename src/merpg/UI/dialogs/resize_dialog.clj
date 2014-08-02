(ns merpg.UI.dialogs.resize-dialog
  (:require [merpg.immutable.basic-map-stuff :refer [width height]]
            [seesaw.core :refer :all :exclude [width height]]
            [seesaw.bind :as b]))

(defn resize-dialog [Map]
  (let [f (frame)
        map-w (width Map)
        map-h (height Map)
        Width (atom map-w )
        Height (atom map-h )
        horizontal-anchor (atom :left )
        vertical-anchor (atom :top )
        ready-state (atom :not-set)]
    (-> f
        (config! :content
                 (grid-panel :columns 2
                             :items
                             (->
                              (zipmap
                               ["Width"
                                "Height"
                                "Horizontal anchor"
                                "Vertical anchor"]
                               [(text :id :width
                                      :text map-w)
                                (text :id :height
                                      :text map-h)
                                (combobox :id :horizontal-anchor
                                          :model [:left :right])
                                (combobox :id :vertical-anchor
                                          :model [:top :bottom])])
                              seq
                              flatten
                              reverse
                              (concat [(button :text "Cancel"
                                               :listen
                                               [:action (fn [_]
                                                          (reset! ready-state :cancel)
                                                          (hide! f))])
                                       (button :text "Ok"
                                               :listen
                                               [:action (fn [e]
                                                          (reset! ready-state :ok)
                                                          (hide! f))])]))))
        pack!
        show!)
    (listen f :window-closed
            (fn [_]
              (if (= @ready-state :not-set)
                (reset! ready-state :cancel))))

    (let [old-w (atom 0)]
      (b/bind (select f [:#width])
              (b/transform #(do
                              (try
                                (let [new-w (Long/parseLong %)]
                                  (reset! old-w new-w))
                                (catch NumberFormatException ex))
                              @old-w))
              Width))
    (let [old-h (atom 0)]
      (b/bind (select f [:#height])
              (b/transform #(do
                              (try
                                (let [new-h (Long/parseLong %)]
                                  (reset! old-h new-h))
                                (catch NumberFormatException ex))
                              @old-h))
              Height))
    (b/bind (b/selection (select f [:#horizontal-anchor])) horizontal-anchor)
    (b/bind (b/selection (select f [:#vertical-anchor])) vertical-anchor)
    {:width Width
     :height Height
     :horizontal-anchor horizontal-anchor
     :vertical-anchor vertical-anchor
     :ready-state ready-state}))
