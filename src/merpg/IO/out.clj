(ns merpg.IO.out
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [merpg.IO.tileset :refer :all]
            [merpg.UI.askbox :refer [in?]])
  (:import [java.util.zip ZipEntry ZipOutputStream ZipInputStream ZipFile]
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
          


(defn dump-image [filename registry-snapshot rendered-tilesets] 
  (try
    (let [registry-snapshot (->> (dissoc registry-snapshot nil)
                                 (filter #(not (in? [:tool :tileset] (-> %
                                                                         second
                                                                         :type))))
                                 (into {}))
          filename (if (.endsWith filename ".zip")
                     filename
                     (str filename ".zip"))]
      (with-open [file (io/output-stream filename)
                  zip  (ZipOutputStream. file)
                  wrt  (io/writer zip)]
        (binding [*out* wrt]
          (doto zip
            (with-entry "registry" _
              (pr registry-snapshot))))
        (doseq [[key tileset] rendered-tilesets]
          (doto zip
            (with-entry (str key ".png") zipfile
              (ImageIO/write tileset "png" zipfile)))))
      
      (.renameTo (io/file filename)
                 (io/file (str/replace filename ".zip" ".memap"))))
    (println "Saved " (str/replace filename ".zip" ".memap"))
    true
    (catch Exception ex
      (pprint ex)
      false)))

(defn entries [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn rdr-slurp [rdr]
  (loop [content ""]
    (if-let [line (.readLine rdr)]
      (recur (str content line))
      content)))

(defn read-image [filename]
  (let [map-list-atom (atom [])
        tileset-map-atom (atom {})
        filename (if (.endsWith filename ".memap")
                   filename
                   (str filename ".memap"))]
    (with-open [zip (ZipFile. filename)]
      (doseq [entry (entries zip)]
        (cond
          (.endsWith (.getName entry) ".memap") (with-open [in-stream (.getInputStream zip entry)
                                                            rdr (io/reader in-stream)]
                                                  (let [content (rdr-slurp rdr)]
                                                    (swap! map-list-atom conj (read-string content))))
          (.endsWith (.getName entry) ".png") (with-open [in-stream (.getInputStream zip entry)]
                                                (let [name (keyword (str/replace (.getName entry) #".png" ""))]
                                                  (swap! tileset-map-atom assoc name (img-to-tileset (ImageIO/read in-stream)))))
          true (locking *out*
                 (println "Found unrecognized data-entry with name " (.getName entry) " in file " filename)))))
    {:maps @map-list-atom
     :tilesets @tileset-map-atom}))
