(ns merpg.mutable.relocation
  (:require [merpg.macros.multi :refer [def-real-multi]]))

(comment An idea of syntac
         (defn-reloc [old-map side]
           (...dostuff..)
           (...edit the atoms def'd in merpg.UI.main-layout)
           return nil)
         
         And example of intermediatery form
         (defn symbol# [old-map side]
           (dostuff)
           ...etc
           nil))

;; First keys are the map's ids, second keys are the sides [:north, :south etc]
(def reloc-map (atom {}))

(defmacro defn-reloc [[old-map-id side] & code]
  `(do
     (defn reloc# [~'old-map-id ~'side]
      ~@code)
    (swap! reloc-map assoc-in [~old-map-id ~side] reloc#)))


(comment
  to set up a relocation fn for a map, call the previous macro like this
  (defn-reloc [the-map-you-wish-to-bind-the-relocation
               edge] ;; defined to be one of #{:north :south :east :west}
    (here be)
    (code that mucks about)
    (the atoms of merpg.UI.main-layout)))
