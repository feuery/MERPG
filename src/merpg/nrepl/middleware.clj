(ns merpg.nrepl.middleware
  (:require [clojure.tools.nrepl.transport :as t]
            [clojure.tools.nrepl.misc :refer (response-for)]
            [clojure.tools.nrepl.middleware :refer (set-descriptor!)]

            [clojure.pprint :refer :all]
            [clojure.string :as str]

            [merpg.mutable.registry :as re]))

(defn handle-find-file [op transport ns msg]
  (let [ns (symbol ns)
        script-assets (re/query! #(and (= (:type %) :script)
                                       (= (:ns %) ns)))
        notes (atom [])
        the-asset (-> script-assets first second)
        {:keys [debug?]} msg
        debug? (= debug? "true")]
    
    (if (> (count script-assets) 1)
      (swap! notes conj (str "Found " (count script-assets) " scripts in this ns. Errors are probable. Use only one script asset per ns. Returning the first found")))

    (when debug?
      (swap! notes conj (str "The asset is: " (pr-str the-asset)))
      (swap! notes conj (str "Amount of assets is " (count script-assets)))
      (swap! notes conj (str "Ns is " ns))
      (swap! notes conj (str "Amount of scripts is " (count (re/query! #(= (:type %) :script))))))

    (swap! notes conj (str "Found a script with ns " ns))
    
    (t/send transport
            (response-for msg
                          :status :done
                          :contents (:src the-asset)
                          :notes (str/join "\n" @notes)))))

(defn handle-save-file [op transport ns msg]
  (let [{:keys [contents]} msg
        ids (->> (re/query! #(and (= (:type %) :script)
                                  (= (:ns %) (symbol ns))))
                 keys)
        notes (atom "")]
    (doseq [id ids]
      (re/update-registry id
                          (assoc id :src contents)))
    (swap! notes conj (str "Saved ns " ns " (" (count ids) ") assets saved"))

    (t/send transport
          (response-for msg
                        :status :done
                        :notes (str/join "\n" @notes)))))

(defn find-file-handler [h]
  (fn [{:keys [op transport ns] :as msg}]
    (if (= "find-file" op)
      (handle-find-file op transport ns msg)
      (if (= "save-file" op)
        (handle-save-file op transport ns msg)
        (h msg)))))

(set-descriptor! #'find-file-handler
                 {:requires #{}
                  :expects #{"eval"}
                  :handles {"find-file"
                            {:doc "Handles find-file on merpg's script assets"
                             :returns {"file" "Contents of the script file"}}
                            "save-file" {}}})
