(ns merpg.immutable.basic-map-stuff
  (:require [merpg.macros.multi :refer :all]
            [clojure.test :refer :all]
            [clojure.pprint :refer :all]))

(defn tile [x y tileset rotation]
  {:x x :y y :tileset tileset :rotation rotation})

(defn make-layer [w h & {:keys [opacity] :or {opacity 255}}]
  (with-meta
    (->> (tile 0 0 0 0)
        (repeat h)
        vec
        (repeat w)
        vec)
    {:tyyppi :layer
     :name "New layer"
     :opacity opacity
     :visible? true}))

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
  [w h layercount & {:keys [north west south east]
                     :or
                     {north (fn [& _] ) west (fn [& _] ) south (fn [& _] ) east (fn [& _] )}}]
  (with-meta (vec (repeat layercount (make-layer w h)))
    {:tyyppi :map
     :relocation-fns {:north north :west west :south south :east east}}))


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

(defmacro with-meta-of [from to]
  `(with-meta ~to (meta ~from)))

(def-real-multi rewidth [thing new-w & {:keys [anchor] :or {anchor :right}}]
  (if (= (-> thing meta :tyyppi) :layer)
      {:type (-> thing meta :tyyppi)
       :append? (> new-w (width thing))
       :anchor anchor}
      {:type (-> thing meta :tyyppi)}))

(defmethod rewidth {:type :layer :append? true :anchor :right}
  [layer new-w & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))]}
  (with-meta-of layer (vec (concat layer (->> (tile 0 0 0 0)
                                (repeat (height layer))
                                vec
                                (repeat (- new-w (width layer))))))))
(defmethod rewidth {:type :layer :append? true :anchor :left}
  [layer new-w & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))]}
  (with-meta-of layer (vec (concat (->> (tile 0 0 0 0)
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
          (not (= layer %))
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map (fn [row] (concat (->> (tile 0 0 0 0)
                                                       (repeat (- new-h (height layer)))
                                                       vec)
                                                  row)))
                           (map vec)
                           vec)))
(defmethod reheight {:type :layer :append? true :anchor :bottom}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          (not (= layer %))
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map (fn [row] (concat row
                                                  (->> (tile 0 0 0 0)
                                                       (repeat (- new-h (height layer)))
                                                       vec))))
                           (map vec)
                           vec)))

(defmethod reheight {:type :layer :append? false :anchor :top}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          (not (= layer %))
          (not (= (height layer) (height %)))]}
  (with-meta-of layer (->> layer
                           (map #(drop (- (height layer) new-h) %))
                           (map vec)
                           vec)))

(defmethod reheight {:type :layer :append? false :anchor :bottom}
  [layer new-h & _]
  {:post [(not (= (class %) clojure.lang.LazySeq))
          (not (= layer %))
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
