(ns ragtime.sql
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [resauce.core :as resauce]))

(defn- file-extension [file]
  (re-find #"\.[^.]*$" (str file)))

(let [pattern (re-pattern (str "([^\\/]*)\\/?$"))]
  (defn- basename [file]
    (second (re-find pattern (str file)))))

(defn- remove-extension [file]
  (second (re-matches #"(.*)\.[^.]*" (str file))))

(defmulti load-files
  "Given an collection of files with the same extension, return a ordered
  collection of migrations. Dispatches on extension (e.g. \".edn\"). Extend
  this multimethod to support new formats for specifying SQL migrations."
  (fn [files] (file-extension (first files))))

(defmethod load-files :default [_files])

(defmethod load-files ".edn" [files]
  (for [file files]
    (-> (slurp file)
        (edn/read-string)
        (update-in [:id] #(or % (-> file basename remove-extension)))
        (update-in [:transactions] (fnil identity :both)))))

(defn- sql-file-parts [file]
  (rest (re-matches #"(.*?)\.(up|down)(?:\.(\d+))?\.sql" (str file))))

(defn- read-sql [file]
  (str/split (slurp file) #"(?m)\n\s*--\s?;;\s*\n"))

(defmethod load-files ".sql" [files]
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
       (mapcat load-files)
       (sort-by :id)))

(defn load-directory
  "Load a collection of Ragtime migrations from a directory."
  [path]
  (->> (file-seq (io/file path))
       (map #(.toURI ^java.io.File %))
       (load-all-files)))

(defn load-resources
  "Load a collection of Ragtime migrations from a classpath prefix."
  [path]
  (load-all-files (resauce/resource-dir path)))
