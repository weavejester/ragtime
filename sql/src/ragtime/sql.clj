(ns ragtime.sql
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ragtime.sql.compiler :as compiler]
            [resauce.core :as resauce]))

(defn- wrap-single-migration [migration]
  (if (map? migration) [migration] migration))

(defn- normalize-migration [migration]
  (update migration :transactions (fnil identity :both)))

(defn- file-extension [file]
  (re-find #"\.[^.]*$" (str file)))

(let [pattern (re-pattern (str "([^\\/]*)\\/?$"))]
  (defn- basename [file]
    (second (re-find pattern (str file)))))

(defn- remove-extension [file]
  (second (re-matches #"(.*)\.[^.]*" (str file))))

(defprotocol ToURI
  (to-uri [x]))

(extend-protocol ToURI
  String       (to-uri [s] (.toURI (io/file s)))
  java.io.File (to-uri [f] (.toURI f))
  java.net.URL (to-uri [u] (.toURI u))
  java.net.URI (to-uri [u] u))

(defn- guess-id-from-file-extension [f migrations]
  (if (and (nil? (next migrations))
           (nil? (:id (first migrations)))
           (satisfies? ToURI f))
    [(assoc (first migrations) :id (-> f to-uri str basename remove-extension))]
    migrations))

(defn load-migrations
  "Load one or more migrations from an edn reader source. Returns an ordered
  collection of migration maps."
  [f]
  (->> (slurp f)
       (edn/read-string)
       (wrap-single-migration)
       (compiler/compile)
       (map normalize-migration)
       (guess-id-from-file-extension f)))

(defmulti load-file-seq
  "Given an collection of files with the same extension, return a ordered
  collection of migrations. Dispatches on extension (e.g. \".edn\"). Extend
  this multimethod to support new formats for specifying SQL migrations."
  (fn [files] (file-extension (first files))))

(defmethod load-file-seq :default [_files])

(defmethod load-file-seq ".edn" [files]
  (mapcat load-migrations files))

(defn- sql-file-parts [file]
  (rest (re-matches #"(.*?)\.(up|down)(?:\.(\d+))?\.sql" (str file))))

(defn- read-sql [file]
  (str/split (slurp file) #"(?m)\n\s*--\s?;;\s*\n"))

(defmethod load-file-seq ".sql" [files]
  (for [[id files] (->> files
                        (group-by (comp first sql-file-parts))
                        (sort-by key))]
    (let [{:strs [up down]} (group-by (comp second sql-file-parts) files)]
      {:id   (basename id)
       :up   (vec (mapcat read-sql (sort-by str up)))
       :down (vec (mapcat read-sql (sort-by str down)))})))

(defn- load-all-files [files]
  (->> (group-by file-extension files)
       (vals)
       (mapcat load-file-seq)
       (sort-by :id)))

(defn load-directory
  "Load a collection of Ragtime migrations from a directory. These can be in edn
  or SQL format."
  [path]
  (load-all-files (file-seq (io/file path))))

(defn load-resources
  "Load a collection of Ragtime migrations from a classpath prefix. These can be
  in edn or SQL format."
  [path]
  (load-all-files (resauce/resource-dir path)))
