(ns merpg.settings.nrepl
  (:require [merpg.settings.core :refer :all]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.nrepl.server :as nrepl]
            [merpg.nrepl.middleware :refer [find-file-handler]])
  (:import [java.net ServerSocket BindException]))



(def server (atom nil))

(add-prop-watch :nrepl-running? :start-nrepl
                (fn [start?]
                  (println "At :nrepl-running? - start? is " start?)
                  (if (and start?
                           (not (some? @server)))
                    (let [port (get-prop! :nrepl-port)]
                      (reset! server (nrepl/start-server :port port :handler (find-file-handler cider-nrepl-handler)))
                      (println "nREPL Server running on port " port))
                    (when (and (not start?)
                               (some? @server))
                      (println "Stopping nREPL")
                      (nrepl/stop-server @server)
                      (reset! server nil)))))

(defn server-running!? []
  (some? @server))

(add-prop-watch :nrepl-port :nrepl-restarter
                (fn [port]
                  (println "At :nrepl-restarter")
                  (when (server-running!?)
                    (println "Restarting nrepl")
                    (set-prop! :nrepl-running? false)
                    (set-prop! :nrepl-running? true))))

(add-prop-validator :nrepl-port :port-free?
                    (fn [port]
                      (try
                        (with-open [ss (ServerSocket. port)]
                          (println "Port " port " seems to be free ")
                          true)
                        (catch BindException _
                          (println "Port "  port " isn't free")
                          false))))
