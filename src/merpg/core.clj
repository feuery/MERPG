(ns merpg.core
  (:require [merpg.UI.main-layout :refer [show-mapeditor]]
            [seesaw.core :refer [native!]]
            [clojure.pprint :refer :all]
            [clojure.string :as str]
            [merpg.mutable.tileset]
            [merpg.mutable.maps]
            [merpg.mutable.registry :as re]
            [merpg.settings.core :as settings]
            [merpg.settings.nrepl]
            [merpg.IO.out :refer [read-image!]]
            [merpg.game.core :refer [run-game!]]
            [merpg.reagi :refer :all])
  (:gen-class))

(settings/fire-events!)

(def arg-cmds {"--image" {:doc
                          " --image
Usage: 
java -jar $path-to-merpg.jar --image $path-to-memap-file

Runs the game contained in the memap file"
                          :fn
                          (fn [[_ path]]
                            (println "Reading " path)
                            (if (read-image! path)
                              (println "Read " path)
                              (println "Failure while reading " path))
                            (run-game!))}
               "--help" {:fn (fn [[_ cmd]]
                               (if (contains? arg-cmds cmd)
                                 (do
                                   (println (-> arg-cmds
                                                (get cmd)
                                                :doc)))
                                 (println (->> arg-cmds
                                               (map (fn [[_ cmd]]
                                                      (:doc cmd)))
                                               (filter some?)
                                               (str/join "\n\n")))))
                         :doc "--help 
Usage:
java -jar $path-to-merpg.jar --help [--command]

Prints the help. If --command is supplied, prints only its help, otherwise prints every command's help."}})

(defn -main [& args]  
  (native!)
  (reset! re/render-allowed? true)
  (if (nil? args)
    (do
      (swap! re/registry identity)
      (show-mapeditor))
    (let [[cmd path] args]
      (if-some [fnn (some-> (get arg-cmds cmd)
                            :fn)]
        (fnn args)
        (do
          (println "No command " cmd " found, known commands are: ")
          (pprint (keys arg-cmds)))))))
    

(defn main []
  (-main))

;; (main)
