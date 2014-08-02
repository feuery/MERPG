(ns merpg.UI.BindableList
  (:require [seesaw.core :refer :all]
            [seesaw.bind :as b]))

(defn bindable-list [list-data-atom selected-item-atom &
                     {:keys [custom-model-bind
                             selected-index-atom
                             ;; reverse?
                             on-select]
                      :or {custom-model-bind nil
                           selected-index-atom (atom 0)
                           ;; reverse? false
                           on-select (fn [_])}}]
  (defn create-renderer
    [renderer {:keys [value]}]
    (config! renderer :text (str (custom-model-bind value))))

  (add-watch list-data-atom :bindable-list-watch
             (fn [_ _ _ _]
               (println "list-data changed")))
  
  (let [list (listbox :model ;; (if reverse?
                             ;;   (reverse @list-data-atom)
                               @list-data-atom
                               ;; )        
                      :renderer create-renderer
                      :listen
                      [:mouse-clicked #(when (= (.getClickCount %) 2)
                                         (on-select %))])
        last-good-index (atom 0)] ;;JList fires unselected-events (with index -1) too on the b/selection - event. This fucks up the selected-index-atom binding
    ;; (if reverse?
    ;;   (b/bind list-data-atom
    ;;           (b/transform reverse)
    ;;           (b/property list :model))
    (b/bind list-data-atom
            (b/property list :model));; )
    (comment (b/bind (b/property list :model)
            list-data-atom) ;;bidirectional \o/
             )
    (b/bind (b/selection list) selected-item-atom)
    (b/bind (b/selection list) (b/transform (fn [_]
                                              (let [ind (.getSelectedIndex list)]
                                                (when-not (neg? ind)
                                                  (reset! last-good-index ind))

                                                @last-good-index)))
            selected-index-atom)
    list))
