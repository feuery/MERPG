(ns merpg.mutable.registry
  (:require [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [postwalk]]))

(def ^:dynamic registry (atom {}))

(defn update-element!
  ([id fn]
   (swap! registry update id fn)))

(defn register-element!
  "Returns id"
  ([id element]
   (swap! registry assoc id element)
   id)
  ([element]
   (let [id (gensym)]
     (register-element! id element))))

(defn peek-registry [id]
  (get @registry id))

(defmacro update-registry
  "Swap!s stuff in registry with a function built from the \"forms\" parameter. 
  Within forms you can refer to the registry's element with it's id like this:

(register-element! :age-of-another-obj 3)
(update-registry :age-of-obj
                 (println (str \"Age of object is \" :age-of-object))
                 (println \"Increasing...\")
                 (inc :age-of-obj))

  Currently returns the whole registry. Do not count on the return value though."
  
  [id & forms]
  (let [element-sym (gensym)
        forms (postwalk (fn [e]
                          (if (= e id)
                           element-sym
                           e))
                        forms)]
    `(update-element! ~id
                      (fn [~element-sym]
                        ~@forms))))
