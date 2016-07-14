(ns merpg.UI.events)

(def ^:dynamic *rebuild-dom?* false)

(defmacro allow-events [& forms]
  `(binding [*rebuild-dom?* true]
     ~@forms))
