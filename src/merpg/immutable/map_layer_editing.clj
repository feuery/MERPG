(ns merpg.immutable.map-layer-editing
  (:require [merpg.immutable.basic-map-stuff :refer :all]
            [merpg.macros.multi :refer [def-real-multi]]
            [clojure.pprint :refer :all]
            [clojure.test :refer :all]))

;;; Tile-editing

(def-real-multi get-tile [& params]
  [(-> params first meta :tyyppi)
   (dec (count params))])

(defmethod get-tile [:layer 2]
  [layer x y]
  {:pre [(not (= (class layer) clojure.lang.LazySeq))]}
  (get-in layer [x y]))

(defmethod get-tile [:map 3]
  [map layer x y]
  {:pre [(not (= (class map) clojure.lang.LazySeq))]}
  (get-in map [layer x y]))

(def-real-multi set-tile [& params]
  [(-> params first meta :tyyppi)
   (dec (count params))])

(defmethod set-tile [:layer 3]
  [layer x y tile]
  {:pre [(not (= (class layer) clojure.lang.LazySeq))]}
  (assoc-in layer [x y] tile))

(defmethod set-tile [:map 4]
  [map layer x y tile]
  {:pre [(not (= (class map) clojure.lang.LazySeq))]}
  (assoc-in map [layer x y] tile))

(def-real-multi rotate-tile [& params]
  [(-> params first meta :tyyppi)
   (dec (count params))])

(defmethod rotate-tile [:layer 3]
  [layer x y new-rot]
  (let [new-rot (mod new-rot 4)]
    (assoc-in layer [x y :rotation] new-rot)))

(defmethod rotate-tile [:map 4]
  [map layer x y new-rot]
  (let [new-rot (mod new-rot 4)]
    (assoc-in map [layer x y :rotation] new-rot)))

;; Layer-sorting
(def-real-multi swap-layers [obj i j] (-> obj meta :tyyppi))

(defn swap [col i j] 
  (-> col (assoc i (col j)) (assoc j (col i))))

(defmethod swap-layers :map
  [layer i j]
  (swap layer i j))


;;; Tests
(deftest tile-tests
  (let [m (make-map 2 2 2)
        l (first m)
        t (tile 2 2 2 2)
        new-m (set-tile m 1 0 1 t)
        new-l (set-tile l 1 0 t)
        grown-l (rewidth new-l 5)
        grown-left-l (rewidth new-l 5 :anchor :left)

        shrank-l (rewidth new-l 1)
        shrank-left-l (rewidth new-l 1 :anchor :left)

        grown-m (rewidth new-m 3)
        grown-left-m (rewidth new-m 3 :anchor :left)

        shrank-m (rewidth new-m 1)
        shrank-left-m (rewidth new-m 1 :anchor :left)]
    (are [x y] (= x y)
         (get-tile grown-left-l 4 0) (get-tile grown-l 1 0)

         (get-tile grown-m 1 0 1) (get-tile grown-left-m 1 1 1)

         (get-tile shrank-l 0 1) (tile 0 0 0 0)
         (get-tile shrank-left-l 0 0) (tile 2 2 2 2)

         (width grown-l) 5
         
         (get-tile l 1 0)
         (get-tile m 1 0 1)

         (get-tile shrank-m 1 0 1) (tile 2 2 2 2))))

(deftest shrinking-test
  (let [m (make-map 2 2 2)
        t (tile 2 2 2 2)
        new-m (set-tile m 1 0 1 t)
        shrank-left-m (rewidth new-m 1 :anchor :left)]
    (is
     (->> (flatten shrank-left-m)
          (reduce #(and %1  %2 (tile 0 0 0 0)) true)))))

(deftest horizontal-growing-at-top
  (let [m (-> (make-map 2 2 2)
              (set-tile 1 1 1 (tile 2 2 2 2)))
        m2 (reheight m 3 :anchor :top)]
    (is (=
         (get-tile m 1 1 1)
         (get-tile m2 1 1 2)))))

(deftest horizontal-growing
  (let [m (-> (make-map 2 2 2)
              (set-tile 1 1 1 (tile 2 2 2 2)))
        m2 (reheight m 3)]
    (is (=
         (get-tile m 1 1 1)
         (get-tile m2 1 1 1)))))

(deftest horizontal-shrinking
  (let [m (-> (make-map 2 2 2)
              (set-tile 1 1 1 (tile 2 2 2 2)))
        m2-bottom (reheight m 1)
        m2-top (reheight m 1 :anchor :top)]
    (are [x y] (= x y)
         (get-tile m 1 1 1) (get-tile m2-top 1 1 0)
         (get-tile m2-bottom 1 1 0) (get-tile m2-top 0 0 0))))

(deftest horizontal-shrinking2
  (let [m (-> (make-map 2 2 2)
              (set-tile 1 1 1 (tile 2 2 2 2)))
        m2-bottom (reheight m 1)
        m2-top (reheight m 1 :anchor :top)]
    (is (and (every? #(= % (tile 0 0 0 0)) (flatten m2-bottom))
             (not (every? #(= % (tile 0 0 0 0)) (flatten m2-top)))))))
