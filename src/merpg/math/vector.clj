(ns merpg.math.vector)

(defn dot-product [[^Double x1 ^Double y1] [^Double x2 ^Double y2]]
  (+ (* x1 x2) (* y1 y2)))

(defn vec-len [[^Double x1
                ^Double y1]]
  (Math/sqrt
   (+
    (Math/pow x1 2.0)
    (Math/pow y1 2.0))))

(defn vec-angle
  ([vec1
    vec2]
   (Math/abs
    (Math/acos
     (/ (dot-product vec1 vec2)
        (* (vec-len vec1) (vec-len vec2))))))
  ([[x y]]
   (let [result (-> (Math/atan2 y x)
                    Math/toDegrees
                    float)
         result (if (pos? result)
                  result
                  (+ result 360))]
     result)))
     
       
   
