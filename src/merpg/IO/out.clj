(ns merpg.IO.out
  (:require [clojure.java.io :as io]
            ;; [merpg.2D.core :refer [
            [merpg.IO.tileset :refer :all])
  (:import [java.util.zip ZipEntry ZipOutputStream]
           [javax.imageio ImageIO]))

(defmacro ^:private with-entry
  [zip entry-name zip-name & body]
  `(let [^ZipOutputStream ~zip-name ~zip]
     (.putNextEntry ~zip-name (ZipEntry. ~entry-name))
     ~@body
     (flush)
     (.closeEntry ~zip-name)))

(comment
  "This is how one makes zipfiles with images & plain text"
  (do
    (.createNewFile (io/file "/Users/feuer2/Desktop/testi.zip"))
    (with-open [file (io/output-stream "/Users/feuer2/Desktop/testi.zip")
                zip  (ZipOutputStream. file)
                wrt  (io/writer zip)]
      (let [tile-img (-> "/Users/feuer2/Desktop/tilejuttu.png"
                         load-tileset
                         tileset-to-img)]
        (binding [*out* wrt]
          (doto zip
            (with-entry "jeee.txt" _
              (println "jfdsiiosdfjoifdsjoisdjfi"))
            (with-entry "Tileset0.png" entry-outputstream
              (ImageIO/write tile-img "png" entry-outputstream))))))))
          

(defn dump-image [filename map-list tileset-list]
  (let [counter (atom -1)]
    (with-open [file (io/output-stream (str filename ".zip"))
                zip  (ZipOutputStream. file)
                wrt  (io/writer zip)]
      (binding [*out* wrt]
        (doseq [tileset tileset-list]
          (doto zip
            (with-entry (str "Tileset " (swap! counter inc) ".png") zipfile
              (ImageIO/write (tileset-to-img tileset) "png" zipfile))))
        (reset! counter -1)
        (doseq [map map-list]
          (with-entry (str "Map " (swap! counter inc) ".png") _ _
            (println (str map))))))))






              
