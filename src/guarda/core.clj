(ns guarda.core
  (:gen-class))

(require '[clojure.string :as str])

(defn path-tree
  "Make a path tree ignoring hidden directories"
  [path]
  (let [file (clojure.java.io/file path)]
    (filter #(not (re-seq #"^\." (.getName %))) (file-seq file)))
)

(defn parse-args
  "Parse flags and associated values if any"
  [args]
  (if-not (empty? args)
    (loop [remaining args opts-list []]
      (if (empty? remaining)
        opts-list
        (let [[arg & remaining-args] remaining] 
          (if (str/includes? ["-o", "-hmac", "-path"] arg)
            (recur (drop 1 remaining-args) (conj opts-list {:flag arg :value (first remaining-args)}))
            (recur remaining-args (conj opts-list {:flag arg})))))))
)

(defn -main
  [& args]
  (println (parse-args args)))
