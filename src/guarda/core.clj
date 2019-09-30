(ns guarda.core
  (:gen-class))

(require '[clojure.string :as str])
(require '[clojure.java.io :as io])
(require '[taoensso.nippy :as nippy])

(import 'java.security.MessageDigest)
(import 'java.math.BigInteger)
(import 'javax.crypto.Mac)
(import 'javax.crypto.spec.SecretKeySpec)

(defn is-hidden?
  [pathname]
  (let [normalized-name (str/replace pathname "\\" "/")]
    (reduce #(if (not= %2 nil) true %1) false (re-find #"(\.\w+\/)|(\/\.[\w\s]+$)|(^\.\w+$)" normalized-name))))

(defn path-tree
  "Make a path tree ignoring hidden directories"
  [path]
  (let [file (clojure.java.io/file path)]
    (filter #(not (is-hidden? (.getPath %))) (file-seq file))))

(defn parse-args
  "Parse flags and associated values if any"
  [args]
  (if-not (empty? args)
    (loop [remaining args opts-list #{}]
      (if (empty? remaining)
        opts-list
        (let [[arg & remaining-args] remaining] 
          (if (str/includes? ["-o", "-hmac", "-path"] arg)
            (recur (drop 1 remaining-args) (conj opts-list {:flag arg :value (first remaining-args)}))
            (recur remaining-args (conj opts-list {:flag arg}))))))))

(defn get-field
  [params field]
  (reduce #(or (if (= (:flag %2) field) %2) %1) nil params))

(defn get-mode
  "Extract the operation mode from params setable"
  [params]
  (or (get-field params "-i") (get-field params "-t") (get-field params "-x")))

; Extracted from https://stackoverflow.com/questions/10062967/clojures-equivalent-to-pythons-encodehex-and-decodehex#10065003
(defn bytes->hex "Convert byte sequence to hex string" [coll]
  (let [hex [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f]]
      (letfn [(hexify-byte [b]
        (let [v (bit-and b 0xFF)]
          [(hex (bit-shift-right v 4)) (hex (bit-and v 0x0F))]))]
        (apply str (mapcat hexify-byte coll)))))

(defn hash-file
  ([file] (let [md (. MessageDigest getInstance "MD5")
      str-file (slurp file)
    ]
    (bytes->hex (.digest md (.getBytes str-file)))
  ))
  ([file key] (let [hmac-md5 (. Mac getInstance "HmacMD5")
      secret-key (new SecretKeySpec (.getBytes (str key)) "HmacMD5")
      str-file (slurp file)
    ]
    (.init hmac-md5 secret-key)
    (bytes->hex (.doFinal hmac-md5 (.getBytes str-file)))
  )))

(defn mount-structure [files & [key]]
  (reduce
    #(assoc %1 (.getPath %2) (if (not= key nil) (hash-file %2 key) (hash-file %2)))
    (hash-map)
    (filter #(not (.isDirectory %)) files)))

(defn already-initialized [path]
  (.exists (io/file (str path "/.guarda"))))

(defn store-structure* [structure path]
  (with-open [wrtr (io/writer (str path "/.guarda/meta"))]
    (.write wrtr (String. (nippy/freeze structure)))))

(defn load-structure* [path]
  (nippy/thaw (.getBytes (slurp (str path "/.guarda/meta")))))

(defn create-guarda-directory* [path]
  (.mkdir (io/file (str path "/.guarda"))))

(defn init-mode [params] (let 
  [path (:value (get-field params "-path"))
   struct (mount-structure (path-tree path) (get-field params "-hmac"))]
    (if-not (already-initialized path)
      (do (create-guarda-directory* path) (store-structure* struct path))
      (println "Guarda already initialized"))))

(defn spot-new-files [old-struct new-struct]
  (reduce (fn [prev curr] 
    (if-not (get old-struct curr) (println (str curr " is a new file.")))
  ) nil (keys new-struct))
)

(defn spot-changed-files [old-struct new-struct]
  (reduce (fn [prev curr] 
    (if (and (get old-struct curr) (not= (get old-struct curr) (get new-struct curr))) (println (str curr " has been changed.")))
  ) nil (keys new-struct)))

(defn spot-removed-files [old-struct new-struct]
  (reduce (fn [prev curr] 
    (if-not (get new-struct curr) (println (str curr " has been removed.")))
  ) nil (keys old-struct)))

(defn track-mode [params] (let [path (:value (get-field params "-path"))]
  (if (already-initialized path)
    (let [base-struct (load-structure* path) current-struct (mount-structure (path-tree path) (get-field params "-hmac"))] 
      (do (spot-new-files base-struct current-struct)
        (spot-changed-files base-struct current-struct)
        (spot-removed-files base-struct current-struct)))
    (println "Guarda is not initialized in this directory"))))

(defn exterminate-mode [params] 
  (let [path (:value (get-field params "-path"))]
    (if (already-initialized path)
      (do (io/delete-file (str path "/.guarda/meta"))
        (io/delete-file (str path "/.guarda"))
        (println "Guarda has been successfully removed from this directory."))
      (println "Guarda is not initialized in this directory."))))

(defn no-mode [] (println "No mode passed"))

(defn execute-mode [params mode]
  (case mode
    "-i" (init-mode params)
    "-t" (track-mode params)
    "-x" (exterminate-mode params)
    (no-mode)))

(defn -main [& args]
  (let [params (parse-args args)] 
    (execute-mode params (:flag (get-mode params)))))
