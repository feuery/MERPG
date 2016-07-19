(ns merpg.settings.core
  (:require [clojure.java.io :as io]
            [merpg.util :refer [mapvals]]))

(defn settings-filename []
  (-> (System/getProperty "user.home")
      (str "/.merpg")))

(def initial-settings {:nrepl-running? false
                       :nrepl-port 33000
                       :meta {:nrepl-port {:max 99999}}})

(defn get-settings []
  (let [conf-file-name (settings-filename)]
    (when-not (-> conf-file-name
                  io/file
                  .exists)
      (spit conf-file-name (str ";;; -*- mode: clojure; -*-
" (pr-str initial-settings))))
    (let [toret (-> conf-file-name
                    slurp
                    read-string
                    eval)]
      toret)))

(def settings (atom (merge initial-settings (get-settings)) :validator some?))

(def setting-watchers (atom {}))

(def setting-validators (atom {}))

(add-watch settings :settings-to-disk
           #(if-not (empty? %4)
              (-> (settings-filename)
                  (spit (str ";;; -*- mode: clojure; -*-\n" %4)))))

(defn set-prop! [k v]
  (let [validated-set (->> (get @setting-validators k)
                           (mapvals (fn [f]
                                      (f v)))
                           vals)
        validated? (or (empty? validated-set)
                       (reduce (fn [a b] (and a b)) validated-set))]
    (when validated?
      (swap! settings assoc k v)
      (doseq [[_ watch-fn] (get @setting-watchers k)]
        (watch-fn v)))
    (when-not validated?
      (println "Validation for setting " k " with value " v " (" (class v) ") failed"))))

(defn get-prop! [k & {:keys [or] :or {or nil}}]
  (get @settings k or))

(defn add-prop-validator [settings-key watch-key fun]
  (if-not (contains? @setting-validators settings-key)
    (swap! setting-validators assoc settings-key {}))
  
  (swap! setting-validators update settings-key assoc watch-key fun))

(defn drop-prop-validator [settings-key watch-key]
  (swap! setting-validators update settings-key dissoc watch-key))
  
(defn add-prop-watch [settings-key watch-key fun]
  (if-not (contains? @setting-watchers settings-key)
    (swap! setting-watchers assoc settings-key {}))
  
  (swap! setting-watchers update settings-key assoc watch-key fun))

(defn drop-prop-watch [settings-key watch-key]
  (swap! setting-watchers update settings-key dissoc watch-key))

(defn fire-events!
  "Basically (swap! settings identity)es the settings. Ran to fire off events"
  []
  (doseq [[k v] @settings]
    (set-prop! k v)))
