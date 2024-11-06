(ns ragtime.jdbc
  "Functions for loading SQL migrations and applying them to a SQL database."
  (:refer-clojure :exclude [load-file])
  (:require [clojure.java.jdbc :as jdbc]
            [ragtime.protocols :as p]
            [ragtime.sql :as sql])
  (:import [java.sql Connection]
           [java.text SimpleDateFormat]
           [java.util Date]))

(defn- migrations-table-ddl [table-name]
  (jdbc/create-table-ddl table-name
                        [[:id "varchar(255)" "primary key"]
                         [:created_at "varchar(32)"]]))

(defn- get-table-metadata* [^Connection conn]
  (with-open [tables (-> conn
                         (.getMetaData)
                         (.getTables (.getCatalog conn) nil "%" nil))]
    (jdbc/metadata-result tables)))

(defn- get-table-metadata [db-spec]
  (if-let [conn (jdbc/db-find-connection db-spec)]
    (get-table-metadata* conn)
    (with-open [conn (jdbc/get-connection db-spec)]
      (get-table-metadata* conn))))

(defn- metadata-matches-table? [^String table-name metadata]
  (.equalsIgnoreCase table-name
                     (if (.contains table-name ".")
                       (str (:table_schem metadata) "." (:table_name metadata))
                       (:table_name metadata))))

(defn- table-exists-via-metadata-scan? [db-spec ^String table-name]
  (some (partial metadata-matches-table? table-name)
        (get-table-metadata db-spec)))

(defn- table-exists-via-provided-sql? [db-spec sql]
  (pos? (count (jdbc/query db-spec [sql]))))

(defn- table-exists? [datasource table-name migrations-table-exists-sql]
  (if migrations-table-exists-sql
    (table-exists-via-provided-sql? datasource migrations-table-exists-sql)
    (table-exists-via-metadata-scan? datasource table-name)))

(defn- ensure-migrations-table-exists
  [db-spec migrations-table migrations-table-exists-sql]
  (when-not (table-exists? db-spec migrations-table migrations-table-exists-sql)
    (jdbc/execute! db-spec [(migrations-table-ddl migrations-table)])))

(defn- format-datetime [^Date dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase [db-spec migrations-table migrations-table-exists-sql]
  p/DataStore
  (add-migration-id [_ id]
    (ensure-migrations-table-exists db-spec migrations-table
                                    migrations-table-exists-sql)
    (jdbc/insert! db-spec migrations-table
                 {:id         (str id)
                  :created_at (format-datetime (Date.))}))

  (remove-migration-id [_ id]
    (ensure-migrations-table-exists db-spec migrations-table
                                    migrations-table-exists-sql)
    (jdbc/delete! db-spec migrations-table ["id = ?" id]))

  (applied-migration-ids [_]
    (ensure-migrations-table-exists db-spec migrations-table
                                    migrations-table-exists-sql)
    (jdbc/query db-spec
               [(str "SELECT id FROM " migrations-table " ORDER BY created_at")]
               {:row-fn :id})))

(defn sql-database
  "Given a db-spec and a map of options, return a Migratable database.
  The following options are allowed:

  :migrations-table - the name of the table to store the applied migrations
                      (defaults to ragtime_migrations)

  :migrations-table-exists-sql - SQL string to check if migrations table
                                 exists. If not supplied, database metadata is
                                 used instead, which can be slow for large DBs."
  ([db-spec]
   (sql-database db-spec {}))
  ([db-spec options]
   (->SqlDatabase db-spec
                  (:migrations-table options "ragtime_migrations")
                  (:migrations-table-exists-sql options))))

(defn- execute-sql! [db-spec statements transaction?]
  (doseq [s statements]
    (jdbc/execute! db-spec [s] {:transaction? transaction?})))

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

(defn load-directory
  "Load a collection of Ragtime migrations from a directory."
  [path]
  (map sql-migration (sql/load-directory path)))

(defn load-resources
  "Load a collection of Ragtime migrations from a classpath prefix."
  [path]
  (map sql-migration (sql/load-resources path)))
