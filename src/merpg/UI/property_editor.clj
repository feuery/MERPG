(ns merpg.UI.property-editor
  (:require [seesaw.core :refer :all]))

(defn property-editor [property-atom & {:keys [with-meta?]
                                        :or {with-meta? false}}]
  (try
    (frame :size [320 :by 240]
           :title "Propertyeditor"
           :visible? true
           
           :content (grid-panel :columns 2 :items
                                (-> (map (fn [[key val]]
                                           (let [serialize (fn [e]
                                                             (println "Swapping " key " to " (text e) " on " property-atom " with-meta? " with-meta)
                                                             (if with-meta?
                                                               (swap! property-atom
                                                                      (fn [old]
                                                                        (vary-meta old assoc key 
                                                                                   (cond
                                                                                    (and
                                                                                     (not= (text e) "")
                                                                                     (number? val)) (Long/parseLong (text e))
                                                                                     (keyword? val) val
                                                                                     (= (class val) java.lang.Boolean) (selection e)
                                                                                     :t (text e)))))
                                                               (swap! property-atom assoc key
                                                                      (cond
                                                                       (and
                                                                        (not= (text e) "")
                                                                        (number? val)) (Long/parseLong (text e))
                                                                        (keyword? val) val
                                                                        (= (class val) java.lang.Boolean) (selection e)
                                                                        :t (text e)))))]
                                             [(str key)
                                              (if (not= (class val) java.lang.Boolean)
                                                (text :text (str val)
                                                      :listen [:document serialize])
                                                (checkbox :selected? val
                                                          :listen [:action-performed serialize]))]))
                                         (filter #(not (keyword? (second %))) (if with-meta?
                                                                                (meta @property-atom)
                                                                                @property-atom)))
                                         flatten
                                         vec)))
    (catch Exception ex
      (println "Plöp")
      (println ex))))
