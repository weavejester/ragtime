(ns ragtime.jdbc
  "Functions for loading SQL migrations and applying them to a SQL database."
  (:refer-clojure :exclude [load-file])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.string :as str]
            [ragtime.protocols :as p]
            [resauce.core :as resauce])
  (:import [java.text SimpleDateFormat]
           [java.util Date]))

(defn- migrations-table-ddl [table-name]
  (sql/create-table-ddl table-name
                        [[:id "varchar(255)"]
                         [:created_at "varchar(32)"]]))

(defn- get-table-metadata* [conn]
  (-> conn
      (.getMetaData)
      (.getTables (.getCatalog conn) nil "%" nil)
      (sql/metadata-result)))

(defn- get-table-metadata [db-spec]
  (if-let [conn (sql/db-find-connection db-spec)]
    (get-table-metadata* conn)
    (with-open [conn (sql/get-connection db-spec)]
      (get-table-metadata* conn))))

(defn- metadata-matches-table? [^String table-name metadata]
  (.equalsIgnoreCase table-name
                     (if (.contains table-name ".")
                       (str (:table_schem metadata) "." (:table_name metadata))
                       (:table_name metadata))))

(defn- table-exists? [db-spec ^String table-name]
  (some (partial metadata-matches-table? table-name)
        (get-table-metadata db-spec)))

(defn- ensure-migrations-table-exists [db-spec migrations-table]
  (when-not (table-exists? db-spec migrations-table)
    (sql/execute! db-spec [(migrations-table-ddl migrations-table)])))

(defn- format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase [db-spec migrations-table]
  p/DataStore
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
               {:row-fn :id})))

(defn sql-database
  "Given a db-spec and a map of options, return a Migratable database.
  The following options are allowed:

  :migrations-table - the name of the table to store the applied migrations
                      (defaults to ragtime_migrations)"
  ([db-spec]
   (sql-database db-spec {}))
  ([db-spec options]
   (->SqlDatabase db-spec (:migrations-table options "ragtime_migrations"))))

(defn- execute-sql! [db-spec statements transaction?]
  (doseq [s statements]
    (sql/execute! db-spec [s] {:transaction? transaction?})))

(defrecord SqlMigration [id up down transactions]
  p/Migration
  (id [_] id)
  (run-up! [_ db]
    (execute-sql! (:db-spec db)
                  up
                  (contains? #{:up :both true} transactions)))
  (run-down! [_ db]
    (execute-sql! (:db-spec db)
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

(defn- sql-file-transactions [sql-migration-map]
  (let [{:keys [up down]} sql-migration-map
        is-up-transaction (-> up first (clojure.string/starts-with? "--transaction-off"))
        is-down-transaction (-> down first (clojure.string/starts-with? "--transaction-off"))
        transactions (case [is-up-transaction is-down-transaction]
                       [true true] :false
                       [true false] :down
                       [false true] :up
                       :both)]
    (assoc sql-migration-map :transactions transactions)))

(defmethod load-files ".sql" [files]
  (for [[id files] (->> files
                        (group-by (comp first sql-file-parts))
                        (sort-by key))]
    (let [{:strs [up down]} (group-by (comp second sql-file-parts) files)]
      (->
       {:id   (basename id)
        :up   (vec (mapcat read-sql (sort-by str up)))
        :down (vec (mapcat read-sql (sort-by str down)))}
       sql-migration))))

(defn- load-all-files [files]
  (->> (group-by file-extension files)
       (vals)
       (mapcat load-files)
       (sort-by :id)))

(defn load-directory
  "Load a collection of Ragtime migrations from a directory."
  [path]
  (load-all-files (map #(.toURI %) (file-seq (io/file path)))))

(defn load-resources
  "Load a collection of Ragtime migrations from a classpath prefix."
  [path]
  (load-all-files (resauce/resource-dir path)))
