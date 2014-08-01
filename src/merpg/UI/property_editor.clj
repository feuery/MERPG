(ns merpg.UI.property-editor
  (:require [seesaw.core :refer :all]
            [clojure.stacktrace :refer [print-stack-trace]]))

(def not-keyword? #(not (keyword? (second %))))

(defn filter-kws [thing with-meta?]
  (filter not-keyword? (if with-meta?
                         (meta thing)
                         thing)))

(defn property-editor [property-atom & {:keys [with-meta? index]
                                        :or {with-meta? false
                                             index nil}}]
  (try
    (frame :size [320 :by 240]
           :title "Propertyeditor"
           :visible? true
           
           :content (grid-panel :columns 2 :items
                                (let [properties (filter-kws
                                                  (if (nil? index)
                                                    @property-atom
                                                    (get @property-atom index))
                                                  with-meta?)]
                                  (println "props: " properties)
                                  (-> (map (fn [[key val]]
                                             (let [serialize (fn [e]
                                                               ;; (println "Swapping " key " to " (text e) " on " property-atom " with-meta? " with-meta)
                                                               (println "Interesting swapping state [index with-meta?]: " [index with-meta?])
                                                               (if with-meta?
                                                                 (swap! property-atom           ;;Hän swappaa nyt kyllä jotain ihan muuta kuin pitäisi, koska piirtorutiini löytää jostain nullin...
                                                                        (fn [old-main-atom]
                                                                          (if (nil? index)
                                                                            (vary-meta old-main-atom assoc key 
                                                                                       (cond
                                                                                        (and
                                                                                         (not= (text e) "")
                                                                                         (number? val)) (Long/parseLong (text e))
                                                                                         (keyword? val) val
                                                                                         (= (class val) java.lang.Boolean) (selection e)
                                                                                         :t (text e)))
                                                                            
                                                                            (assoc-in old-main-atom [index]
                                                                                      (vary-meta (get old-main-atom index) assoc key
                                                                                                 (cond
                                                                                                  (and
                                                                                                   (not= (text e) "")
                                                                                                   (number? val)) (Long/parseLong (text e))
                                                                                                   (keyword? val) val
                                                                                                   (= (class val) java.lang.Boolean) (selection e)
                                                                                                   :t (text e))))     ;; Tämä hajottaa?

                                                                            )))
                                                                 (swap! property-atom
                                                                        (fn [old]
                                                                          (if (nil? index)
                                                                            (assoc old key
                                                                                   (cond
                                                                                    (and
                                                                                     (not= (text e) "")
                                                                                     (number? val)) (Long/parseLong (text e))
                                                                                     (keyword? val) val
                                                                                     (= (class val) java.lang.Boolean) (selection e)
                                                                                     :t (text e)))
                                                                            (assoc-in old [index]
                                                                                      (assoc (get index old) key
                                                                                             (cond
                                                                                              (and
                                                                                               (not= (text e) "")
                                                                                               (number? val)) (Long/parseLong (text e))
                                                                                               (keyword? val) val
                                                                                               (= (class val) java.lang.Boolean) (selection e)
                                                                                               :t (text e)))))))))]
                                               [(str key)
                                                (if (not= (class val) java.lang.Boolean)
                                                  (text :text (str val)
                                                        :listen [:document serialize])
                                                  (checkbox :selected? val
                                                            :listen [:action-performed serialize]))]))
                                           properties)
                                      flatten
                                      vec))))
    (catch Exception ex
      (print-stack-trace ex))))
