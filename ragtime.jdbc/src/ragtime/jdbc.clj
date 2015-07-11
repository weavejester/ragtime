(ns ragtime.jdbc
  "Functions for loading SQL migrations and applying them to a SQL database."
  (:refer-clojure :exclude [load-file])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [ragtime.core :as ragtime]
            [resauce.core :as resauce])
  (:import [java.io File]
           [java.sql SQLException]
           [java.text SimpleDateFormat]
           [java.util Date]))

(defn- migrations-table-ddl [table-name]
  (sql/create-table-ddl table-name
                        [:id "varchar(255)"]
                        [:created_at "varchar(32)"]))

(defn- ensure-migrations-table-exists [db-spec migrations-table]
  (try
    (sql/execute! db-spec [(migrations-table-ddl migrations-table)])
    (catch SQLException _)))

(defn- format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase [db-spec migrations-table]
  ragtime/Migratable
  (add-migration-id [_ id]
    (ensure-migrations-table-exists db-spec migrations-table)
    (sql/insert! db-spec migrations-table
                 {:id         (str id)
                  :created_at (format-datetime (Date.))}))
  
  (remove-migration-id [_ id]
    (ensure-migrations-table-exists db-spec migrations-table)
    (sql/delete! db-spec migrations-table ["id = ?" id]))

  (applied-migration-ids [_]
    (ensure-migrations-table-exists db-spec migrations-table)
    (sql/query db-spec
               [(str "SELECT id FROM " migrations-table " ORDER BY created_at")]
               :row-fn :id)))

(alter-meta! #'->SqlDatabase assoc :no-doc true)
(alter-meta! #'map->SqlDatabase assoc :no-doc true)

(defn sql-database
  "Given a db-spec and a map of options, return a Migratable database.
  The following options are allowed:

  :migrations-table - the name of the table to store the applied migrations
                      (defaults to ragtime_migrations)"
  ([db-spec]
   (sql-database db-spec {}))
  ([db-spec options]
   (->SqlDatabase db-spec (:migrations-table options "ragtime_migrations"))))

(defn- execute-sql! [db-spec statements]
  (doseq [s statements]
    (sql/execute! db-spec [s])))

(defn sql-migration
  "Create a Ragtime migration from a map with a unique :id, and :up and :down
  keys that map to ordered collection of SQL strings."
  [{:keys [id up down]}]
  {:id   id
   :up   #(execute-sql! (:db-spec %) up)
   :down #(execute-sql! (:db-spec %) down)})

(defn- file-extension [file]
  (re-find #"\.[^.]*$" (str file)))

(let [pattern (re-pattern (str "([^" File/separator "]*)" File/separator "?$"))]
  (defn- basename [file]
    (second (re-find pattern (str file)))))

(defn- remove-extension [file]
  (second (re-matches #"(.*)\.[^.]*" (str file))))

(defmulti load-files
  "Given an collection of files with the same extension, return a ordered
  collection of migrations. Dispatches on extension (e.g. \".edn\"). Extend
  this multimethod to support new formats for specifying SQL migrations."
  (fn [files] (file-extension (first files))))

(defmethod load-files :default [files])

(defmethod load-files ".edn" [files]
  (for [file files]
    (-> (slurp file)
        (edn/read-string)
        (update-in [:id] #(or % (-> file basename remove-extension)))
        (sql-migration))))

(defn- sql-file-parts [file]
  (rest (re-matches #"(.*?)\.(up|down)(?:\.(\d+))?\.sql" (str file))))

(defn- read-sql [file]
  (str/split (slurp file) #"(?m)\n\s*--;;\s*\n"))

(defmethod load-files ".sql" [files]
  (for [[id files] (group-by (comp first sql-file-parts) files)]
    (let [{:strs [up down]} (group-by (comp second sql-file-parts) files)]
      (sql-migration
       {:id   (basename id)
        :up   (vec (mapcat read-sql (sort-by str up)))
        :down (vec (mapcat read-sql (sort-by str down)))}))))

(defn- load-all-files [files]
  (->> (sort-by str files)
       (group-by file-extension)
       (vals)
       (mapcat load-files)))

(defn load-directory
  "Load a collection of Ragtime migrations from a directory."
  [path]
  (load-all-files (file-seq (io/file path))))

(defn load-resources
  "Load a collection of Ragtime migrations from a classpath prefix."
  [path]
  (load-all-files (resauce/resource-dir path)))
