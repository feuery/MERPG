(ns merpg.immutable.basic-map-stuff
  (:require [merpg.macros.multi :refer :all]
            [merpg.util :refer [with-meta-of]]
            [merpg.vfs :refer [nodify node-name]]
            [clojure.test :refer :all]
            [clojure.string :refer [join upper-case]]
            [clojure.pprint :refer :all]))

(defn tile [x y tileset rotation]
  {:x x :y y :tileset tileset :rotation rotation})

(defn make-thing [default w h & {:keys [opacity] :or {opacity 255}}]
  (node-name
   (nodify (with-meta
             (->> default
                  (repeat h)
                  vec
                  (repeat w)
                  vec)
             {:tyyppi :layer
              :name "New layer"
              :opacity opacity
              :visible? true})
           :directory? false)
   "New layer"))

(def make-layer (partial #'make-thing (tile 0 0 0 0)))
(defn make-bool-layer [w h & {:keys [opacity ;;because api-compability
                                     default-value]
                              :or {opacity 255
                                   default-value true}}]
  (make-thing default-value w h :opacity opacity))

(defn layer-name
  ([layer]
     (-> layer meta :name))
  ([layer name]
     (vary-meta layer assoc :name name)))

(defn layer-visible
  ([layer]
     (-> layer meta :visible?))
  ([layer visible]
     (vary-meta layer assoc :visible? visible)))

(defn opacity
  "Setter-function overflows when val>255"
  ([layer]
     (-> layer meta :opacity))
  ([layer val]
     (let [val (mod val 256)]
       (vary-meta layer assoc :opacity val))))

(defn make-map
  "The &key params are functions that are called when the player crosses the certain edge of the map.
There'll be a default-fn-generator, which makes fn's that look like the old idea of the reloc-vectors."
  [w h layercount]
  (nodify (with-meta (vec (repeat layercount (make-layer w h)))
    {:tyyppi :map
     :id (gensym)
     :hit-layer (make-bool-layer w h)
     
     :name (str "Map "  (->>
                         (partial rand-int 16)
                         (repeatedly 5)
                         (map (partial format "%x"))
                         join
                         upper-case))})))

(def-real-multi hitdata [& params]
  [(-> params first meta :tyyppi)
   (dec (count params))])

(defmethod hitdata [:map 0] [map]
  (-> map meta :hit-layer))

(defmethod hitdata [:map 1] [map new-layer]
  {:pre [(-> new-layer meta :tyyppi (= :layer))]
   :post [(->> %
         hitdata
         flatten
         (every? (partial instance? java.lang.Boolean)))]}
  (vary-meta map assoc :hit-layer new-layer))

(defn layer-count [map]
  {:pre [(-> map meta :tyyppi (= :map))]}
  (count map))


(defn edge-fn
  ([map edge]
     (-> map meta :relocation-fns edge))
  ([map edge new-fn]
     (vary-meta map assoc-in [:relocation-fns edge] new-fn)))

(def-real-multi width [obj] (-> obj meta :tyyppi))
(def-real-multi height [obj] (-> obj meta :tyyppi))

(defmethod width :layer [obj]
  (count obj))

(defmethod height :layer [obj]
  (count (first obj)))

(defmethod width :map [obj]
  (width (first obj)))

(defmethod height :map [obj]
  (height (first obj)))

(defn get-default-val [layer]
  (if (instance? java.lang.Boolean (get-in layer [0 0]))
    true
    (tile 0 0 0 0)))

(def-real-multi rewidth [thing new-w & {:keys [anchor] :or {anchor :right}}]
  (if (= (-> thing meta :tyyppi) :layer)
      {:type (-> thing meta :tyyppi)
       :append? (> new-w (width thing))
       :anchor anchor}
      {:type (-> thing meta :tyyppi)}))

(defmethod rewidth {:type :layer :append? true :anchor :right}
  [layer new-w & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))]}
  (with-meta-of layer (vec (concat layer (->> layer
                                              get-default-val
                                              (repeat (height layer))
                                              vec
                                              (repeat (- new-w (width layer))))))))

(defmethod rewidth {:type :layer :append? true :anchor :left}
  [layer new-w & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))]}
    (with-meta-of layer (vec (concat (->> layer
                                          get-default-val
                                          (repeat (height layer))
                                          vec
                                          (repeat (- new-w (width layer)))) layer))))

(defmethod rewidth {:type :layer :append? false :anchor :right}
  [layer new-w & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))]}
  (with-meta-of layer
    (vec (take new-w layer))))

(defmethod rewidth {:type :layer :append? false :anchor :left}
  [layer new-w & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))]}
  (with-meta-of layer
    (vec (drop
     (- (width layer) new-w)
     layer))))

;; And for maps
(defmethod rewidth {:type :map}
  [Map new-w & {:keys [anchor] :or {anchor :right}}]
  (with-meta-of Map
    (vec
     (map #(rewidth % new-w :anchor anchor) Map))))


;;And reheighting

(def-real-multi reheight [thing new-h & {:keys [anchor] :or {anchor :bottom}}]
  (if (= (-> thing meta :tyyppi) :layer)
      {:type (-> thing meta :tyyppi)
       :append? (> new-h (height thing))
       :anchor anchor}
      {:type (-> thing meta :tyyppi)}))

(defmethod reheight {:type :layer :append? true :anchor :top}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          ;; (not (= layer %))
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map (fn [row] (concat (->> layer
                                                       get-default-val
                                                       (repeat (- new-h (height layer)))
                                                       vec)
                                                  row)))
                           (map vec)
                           vec)))
(defmethod reheight {:type :layer :append? true :anchor :bottom}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          ;; (not (= layer %))
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map (fn [row] (concat row
                                                  (->> layer
                                                       get-default-val
                                                       (repeat (- new-h (height layer)))
                                                       vec))))
                           (map vec)
                           vec)))

(defmethod reheight {:type :layer :append? false :anchor :top}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          ;; (not (= layer %))    ;; Why should one comment one's stupid postconditions?
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map #(drop (- (height layer) new-h) %))
                           (map vec)
                           vec)))

(defmethod reheight {:type :layer :append? false :anchor :bottom}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          ;; (not (= layer %))
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map #(take new-h %))
                           (map vec)
                           vec)))

;;And again with the maps
(defmethod reheight {:type :map}
  [Map new-h & {:keys [anchor] :or {anchor :bottom}}]
  (with-meta-of Map
    (vec
     (map #(reheight % new-h :anchor anchor) Map))))


;; The resize

(defn- with-meta-of-t
  "-t from the transposed params"
  [to from]
  (with-meta-of from to))

(defn resize [thingy new-width new-height & {:keys [horizontal-anchor vertical-anchor] :or
                                     {horizontal-anchor :left
                                      vertical-anchor :top}}]
  (let [first (if (not= new-width (width thingy))
                (-> thingy
                    (rewidth new-width :anchor horizontal-anchor)
                    (with-meta-of-t thingy))
                thingy)
        first-with-hit (if (not= new-width (width thingy))
                         (hitdata first (rewidth (hitdata first) new-width :anchor horizontal-anchor))
                         first)
        second (if (not= new-height (height first-with-hit))
                 (-> first-with-hit
                     (reheight new-height :anchor vertical-anchor)
                     (with-meta-of-t thingy))
                 first-with-hit)]
    (if (not= new-height (height thingy))
                         (hitdata second (reheight (hitdata second) new-height :anchor vertical-anchor))
                         second)))
    


(deftest basic-map-tests
 (let [op 123
       map (make-map 14 11 3)
       layer (-> (make-layer 12 13)
                 (opacity op))
       grown-layer (rewidth layer 15)
       shrank-layer (rewidth layer 10)]
   (are [x y] (= x y)
         (width layer) 12
         (height layer) 13
         (width map) 14
         (height map) 11
         (opacity layer) op
         (opacity (opacity layer 257)) 1

         (width grown-layer) 15
         (width shrank-layer) 10)))

(deftest map-dimension-reduce-test
  (let [map (make-map 14 11 3)]
    (is (reduce #(and %1
                      (= (width %2) 14)
                      (= (height %2) 11)) true map))))
