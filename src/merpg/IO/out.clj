(ns merpg.IO.out
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [merpg.IO.tileset :refer :all]
            [merpg.mutable.tileset :refer [tileset!]]
            [merpg.mutable.sprites :refer [animation->spritesheet]]
            [merpg.mutable.layers :refer [mapvals]]
            [merpg.UI.askbox :refer [in?]]
            [merpg.mutable.registry :as re]
            [merpg.mutable.tools :as t]
            [merpg.UI.events :as e])
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
    (let [registry-snapshot (dissoc registry-snapshot nil)
          sprites (->> registry-snapshot
                       (re/query #(= (:type %) :sprite)))
          
          registry-snapshot (->> registry-snapshot
                                 (filter #(not (in? [:tool :tileset :sprite] (-> %
                                                                         second
                                                                         :type))))
                                 (into {}))          
          filename (if (.endsWith filename ".zip")
                     filename
                     (str filename ".zip"))
          final-filename (-> filename
                             (str/replace ".zip" ".memap")
                             (str/replace #"(\.memap)+$" ".memap"))]
      (with-open [file (io/output-stream filename)
                  zip  (ZipOutputStream. file)
                  wrt  (io/writer zip)]
        (binding [*out* wrt]
          (doto zip
            (with-entry "registry" _
              (pr registry-snapshot))
            (with-entry "sprite-registry" _
              (pr (->> sprites
                       (mapvals #(do
                                   (let [anim? (= (:type %) :animated)
                                         result (dissoc % :surface
                                                        :frames)]
                                     (if anim?
                                       (assoc result :last-updated 0)
                                       result)))))))))
                                  
          ;; write the sprite registry
          
          
        (doseq [[key tileset] rendered-tilesets]
          (let [name (:name (re/peek-registry key))]
            (doto zip
              (with-entry (str "TILESET - " key " - " name ".png") zipfile
                (ImageIO/write tileset "png" zipfile)))))
        ;; save sprites and spritesheets
        (doseq [[key {:keys [subtype surface frames] :as sprite}] sprites]
          (let [filename (str "SPRITE - " key ".png")]
            (condp = subtype
              :static
              (doto zip
                (with-entry filename file
                  (ImageIO/write surface "png" file)))
              :animated
              (doto zip
                (with-entry filename file
                  (ImageIO/write (animation->spritesheet sprite) "png" file)))))))
      
      (.renameTo (io/file filename)
                 (io/file final-filename)))
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

(defn read-image!
  "This puts stuff directly to the registry"
  [filename]
  ;; we can fake onload transactions because we have only one atom
  ;; it remains to be seen how badly reagi channels shall shit themselves though when registry is emptied under them
  (let [old-registry @re/registry]
    (try
      (reset! re/registry {})
      
      (with-open [zip (ZipFile. filename)]
        (doseq [entry (entries zip)]
          (cond
            (= (.getName entry) "registry")
            (with-open [in-stream (.getInputStream zip entry)
                        rdr (io/reader in-stream)]
              (let [registry (-> rdr
                                 rdr-slurp
                                 read-string)]
                (swap! re/registry merge registry)))
            
            (.endsWith (.getName entry) ".png")
            (with-open [in-stream (.getInputStream zip entry)]
              (let [filename (.getName entry)
                    kw-regex #"^TILESET - :[a-zA-Z0-9_-]* - "
                    ;; in theory, user code execution vulnerability
                    ;; in practice, this app is designed to be ran with the embedded nrepl-server on
                    ;; filename is supposed to be formatted :keyword-id - Name.png and read-string is the easiest way to get the id out because it ignores whatever rubbish comes after the first valid clojure literal
                    ;; getting the tileset's name out will require regex-trickery pockery
                    id (read-string (str/replace filename #"^TILESET - " ""))
                    name (-> filename
                             (str/replace kw-regex "")
                             (str/replace ".png" ""))
                    image (ImageIO/read in-stream)]
                (tileset! id name image)))
            
            true (locking *out*
                   (println "Found unrecognized data-entry with name " (.getName entry) " in file " filename)))))
      (e/allow-events 
       (t/load-default-tools!))
      true
      (catch Exception ex
        (reset! re/registry old-registry)
        (locking *out*
          (pprint ex))
        false))))
    
