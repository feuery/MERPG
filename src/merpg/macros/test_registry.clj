(ns merpg.macros.test-registry
  (:require [merpg.mutable.registry :as re]
            [merpg.mutable.registry-views :as rv]
            [reagi.core :as r]))

(defmacro clear-registry
  "This clears merpg.mutable.registry/registry, recreates the watch function that makes sure merpg.mutable.registry-views/layers-view is populated correctly and returns the registry to its previous value by the end of the scope. 

  Currently layers-view is not in the correct state after running this macro, but that doesn't matter because this is supposed to be used in short tests only. I'll fix this if the need arises."
  [& forms]
  `(let [old-registry# @re/registry]         
     (reset! re/registry {})
     (do
       ~@forms)
     (reset! re/registry old-registry#)))
