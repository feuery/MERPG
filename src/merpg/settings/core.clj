(ns merpg.settings.core
  (:require [clojure.java.io :as io]))

(defn settings-filename []
  (-> (System/getProperty "user.home")
      (str "/.merpg")))

(defn get-settings []
  (let [conf-file-name (settings-filename)]
    (when-not (-> conf-file-name
                  io/file
                  .exists)
      (spit conf-file-name ";;; -*- mode: clojure; -*-
{:nrepl-running? false}"))
    (let [toret (-> conf-file-name
                    slurp
                    read-string
                    eval)]
      toret)))

(def initial-settings {:nrepl-running? false})

(def settings (atom (merge initial-settings (get-settings))))
;; (def setting-watchers (atom {:nrepl-running?
;;                              {:watch-key (fn [val]
;;                                            (if val
;;                                              (stopNrepl!)
;;                                              (startNrepl!)))}}))

(def setting-watchers (atom {}))

(add-watch settings :settings-to-disk
           #(-> (settings-filename)
                (spit (str ";;; -*- mode: clojure; -*-\n" %4))))

(defn set-prop! [k v]
  (swap! settings assoc k v)
  (doseq [[_ watch-fn] (get @setting-watchers k)]
    (watch-fn v)))

(defn get-prop! [k & {:keys [or] :or {or nil}}]
  (get @settings k or))

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
