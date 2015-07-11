(ns merpg.UI.BindableList
  (:require [seesaw.core :refer :all :exclude [width height]]
            [merpg.immutable.basic-map-stuff :as bms]
            [seesaw.bind :as b]
            [clojure.pprint :refer :all]))

(defn bindable-list [list-data-atom selected-item-atom &
                     {:keys [custom-model-bind
                             selected-index-atom
                             ;; reverse?
                             on-select]
                      :or {custom-model-bind identity
                           selected-index-atom (atom 0)
                           ;; reverse? false
                           on-select (fn [_])}}]
  (defn create-renderer
    [renderer {:keys [value]}]
    (config! renderer :text (str (custom-model-bind value))))

  (let [list (listbox :model @list-data-atom
                      :renderer create-renderer
                      :listen
                      [:mouse-clicked #(when (= (.getClickCount %) 2)
                                         (on-select %))])
        last-good-index (atom 0)] ;;JList fires unselected-events (with index -1) too on the b/selection - event. This fucks up the selected-index-atom binding
    
    (b/bind list-data-atom
            (b/property list :model))
    
    (b/bind (b/selection list) selected-item-atom)
    (b/bind (b/selection list) (b/transform (fn [_]
                                              (let [ind (.getSelectedIndex list)]
                                                (if-not (neg? ind)
                                                  (reset! last-good-index ind)
                                                  (println "list-index is neg :("))

                                                @last-good-index)))
            selected-index-atom)
    list))

(defn rotate-keys-vals
  "Makes m-map's keys values and vice versa"
  [m]
  (zipmap (vals m) (keys m)))

;; In this context, map=hashmap
(defn bindable-list-with-map-source [map-atom selected-item-atom &
                                     {:keys [selected-index-atom
                                             on-select]
                                      :or {selected-index-atom (atom 0)
                                           on-select (fn [_])}}]
  (def index->keys-atm (atom {}))
  (let [keyschanged! (fn [_ _ _ new-map]
                       (let [keyset (keys new-map)]
                         (reset! index->keys-atm (zipmap (range (count keyset)) keyset))))]
    (keyschanged! nil nil nil @map-atom)
    (add-watch map-atom :keyschanged keyschanged!)
    (defn create-renderer
      [renderer {:keys [value]}]
      (let [motap-pam (rotate-keys-vals @map-atom)]
          (config! renderer :text (or (str (motap-pam value)) "Value not found"))))
                                   
    (let [list (listbox :model (vals @map-atom)
                        :renderer create-renderer
                        :listen
                        [:mouse-clicked #(when (= (.getClickCount %) 2)
                                           (on-select %))])
          last-good-index (atom :initial :validator #(and (not (nil? %)) (not (number? %))))]
      (add-watch last-good-index :watcher (fn [_ _ _ new]
                                            (locking *out*
                                              (println "Resetting last-good-index with " new))))
      (b/bind map-atom
              (b/transform #(-> % vals))
              (b/property list :model))

      (b/bind (b/selection list) selected-item-atom)
      (b/bind (b/selection list) (b/transform (fn [_]
                                                (let [ind (.getSelectedIndex list)]
                                                  (if-not (neg? ind)
                                                    (let [new-good-index (get @index->keys-atm ind :not-found)]
                                                      (if (= new-good-index :not-found)
                                                        (locking *out*
                                                          (println "No key found for index " ind))
                                                        (reset! last-good-index new-good-index)))
                                                    (println "list-index is neg :("))
                                                  @last-good-index)))
              selected-index-atom)
      list)))
