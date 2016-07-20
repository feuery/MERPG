(ns merpg.nrepl.middleware
  (:require [clojure.tools.nrepl.transport :as t]
            [clojure.tools.nrepl.misc :refer (response-for)]
            [clojure.tools.nrepl.middleware :refer (set-descriptor!)]))

(defn find-file-handler [h]
  (fn [{:keys [op transport] :as msg}]
    (if (= "find-file" op)
      (t/send transport (response-for msg :status :done :contents "(ns lol.core)\n\n(println \"Hello world\")"))
      (h msg))))

(set-descriptor! #'find-file-handler
                 {:requires #{}
                  :expects #{"eval"}
                  :handles {"find-file"
                            {:doc "Handles find-file on merpg's script assets"
                             :returns {"file" "Contents of the script file"}}}})
