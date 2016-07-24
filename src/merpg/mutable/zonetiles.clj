(ns merpg.mutable.zonetiles
  (:require [merpg.mutable.registry :as re]
            [merpg.UI.events :as e]))

(defn zonetile!
  "Zonetiles ARE NOT SAVED in the image. Thus you have to add the zonetile! - definitions inside the script assets!

Example of usage. Save this in a script asset: 

(let [ztile (z/zonetile! #(= (:name %) \"New sprite\")
                         #(and (< (:map-x %) 6)
                               (< (:map-y %) 6))
                         #(println \"Sprite \" @% \" did a thing!\"))]

  (re/set-watch! :selected-map :print-selected-map (fn [s-map]
                                                     (if (= s-map :MAP__16991)
                                                       (println \"New selected map is now \" @s-map)
                                                       (z/drop-zonetile! ztile)))))"
  [sprite-predicate tile-predicate event-fn]
  (e/allow-events
   (let [id (-> "zonetile__" gensym keyword)]
     (re/register-element! id
                           {:type :zonetile
                            :sprite-pred sprite-predicate
                            :tile-predicate tile-predicate
                            :event-fn event-fn}))))

(defn drop-zonetile!
  "Cleaning fn for the zonetiles"
  [id]
  (e/allow-events
   (re/remove-element! id)))
    
