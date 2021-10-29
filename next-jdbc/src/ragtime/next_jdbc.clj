(ns ragtime.next-jdbc
  "Functions for loading SQL migrations and applying them to a SQL database."
  (:refer-clojure :exclude [load-file])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [next.jdbc :as jdbc]
            [next.jdbc.default-options]
            [next.jdbc.result-set :as rs]
            [next.jdbc.sql :as sql]
            [clojure.string :as str]
            [ragtime.protocols :as p]
            [resauce.core :as resauce])
  (:import [java.io File]
           [java.sql Connection]
           [java.text SimpleDateFormat]
           [java.util Date]))

;; Returns unqualified maps as some databases (e.g. Oracle) can only return them
(defn- get-table-metadata* [^Connection c]
  (-> (.getMetaData c)
      (.getTables (.getCatalog c) nil nil (into-array ["TABLE"]))
      (rs/datafiable-result-set c {:builder-fn rs/as-unqualified-lower-maps})))

(defn- get-table-metadata [datasource]
  (cond
    (instance? java.sql.Connection datasource)
    (get-table-metadata* datasource)

    (next.jdbc.default-options/wrapped-connection? datasource)
    (get-table-metadata* (:connectable datasource))

    :else
    (with-open [conn (jdbc/get-connection datasource)]
      (get-table-metadata* conn))))

(defn- metadata-matches-table?
  [^String table-name {:keys [table_schem table_name]}]
  (.equalsIgnoreCase table-name
                     (if (.contains table-name ".")
                       (str table_schem "." table_name)
                       table_name)))

(defn- table-exists? [datasource ^String table-name]
  (some (partial metadata-matches-table? table-name)
    (get-table-metadata datasource)))

(defn- ensure-migrations-table-exists [datasource migrations-table]
  (when-not (table-exists? datasource migrations-table)
    (let [sql (str "create table " migrations-table
                " (id varchar(255), created_at varchar(32))")]
      (jdbc/execute! datasource [sql]))))

(defn- format-datetime [^Date dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase [datasource migrations-table]
  p/DataStore
  (add-migration-id [_ id]
    (ensure-migrations-table-exists datasource migrations-table)
    (sql/insert! datasource migrations-table
      {:id         (str id)
       :created_at (format-datetime (Date.))}))

  (remove-migration-id [_ id]
    (ensure-migrations-table-exists datasource migrations-table)
    (sql/delete! datasource migrations-table ["id = ?" id]))

  (applied-migration-ids [_]
    (ensure-migrations-table-exists datasource migrations-table)
    (let [sql [(str "SELECT id FROM " migrations-table " ORDER BY created_at")]]
      (->> (sql/query datasource sql {:builder-fn rs/as-unqualified-lower-maps})
           (map :id)))))

(defn sql-database
  "Given a datasource and a map of options, return a Migratable database.
  The following options are allowed:

  :migrations-table - the name of the table to store the applied migrations
                      (defaults to ragtime_migrations). You must include the
                      schema name if your DB supports that and you are not
                      using the default one (ex.: myschema.migrations)"
  ([datasource]
    (sql-database datasource {}))
  ([datasource options]
    (->SqlDatabase datasource (:migrations-table options "ragtime_migrations"))))

(defn- execute-sql! [datasource statements transaction?]
  (if transaction?
    (jdbc/with-transaction [tx datasource]
      (doseq [s statements]
        (jdbc/execute! (jdbc/with-options tx (:options datasource)) [s])))
    (doseq [s statements]
      (jdbc/execute! datasource [s]))))

(defrecord SqlMigration [id up down transactions]
  p/Migration
  (id [_] id)
  (run-up! [_ db]
    (execute-sql! (:datasource db)
      up
      (contains? #{:up :both true} transactions)))
  (run-down! [_ db]
    (execute-sql! (:datasource db)
      down
      (contains? #{:down :both true} transactions))))

(defn sql-migration
  "Create a Ragtime migration from a map with a unique :id, and :up and :down
  keys that map to ordered collection of SQL strings."
  [migration-map]
  (map->SqlMigration migration-map))

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

(defmethod load-files :default [files])

(defmethod load-files ".edn" [files]
  (for [file files]
    (-> (slurp file)
        (edn/read-string)
        (update-in [:id] #(or % (-> file basename remove-extension)))
        (update-in [:transactions] (fnil identity :both))
        (sql-migration))))

(defn- sql-file-parts [file]
  (rest (re-matches #"(.*?)\.(up|down)(?:\.(\d+))?\.sql" (str file))))

(defn- read-sql [file]
  (str/split (slurp file) #"(?m)\n\s*--;;\s*\n"))

(defmethod load-files ".sql" [files]
  (for [[id files] (->> files
                        (group-by (comp first sql-file-parts))
                        (sort-by key))]
    (let [{:strs [up down]} (group-by (comp second sql-file-parts) files)]
      (sql-migration
        {:id   (basename id)
         :up   (vec (mapcat read-sql (sort-by str up)))
         :down (vec (mapcat read-sql (sort-by str down)))}))))

(defn- load-all-files [files]
  (->> (group-by file-extension files)
       (vals)
       (mapcat load-files)
       (sort-by :id)))

(defn load-directory
  "Load a collection of Ragtime migrations from a directory."
  [path]
  (load-all-files (map #(.toURI ^File %) (file-seq (io/file path)))))

(defn load-resources
  "Load a collection of Ragtime migrations from a classpath prefix."
  [path]
  (load-all-files (resauce/resource-dir path)))
