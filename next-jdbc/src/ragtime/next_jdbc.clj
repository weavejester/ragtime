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
           [java.sql Connection DatabaseMetaData]
           [java.text SimpleDateFormat]
           [java.util Date]
           [java.util.regex Pattern]))

(defn- get-table-metadata [^DatabaseMetaData metadata ^Connection conn]
  (-> metadata
      (.getTables (.getCatalog conn) nil nil (into-array ["TABLE"]))
      ;; Returns unqualified maps as some databases (e.g. Oracle) can only return them
      (rs/datafiable-result-set conn {:builder-fn rs/as-unqualified-lower-maps})))

(defn- get-identifier-quote [^DatabaseMetaData metadata]
  (let [quote-char (.getIdentifierQuoteString metadata)]
    (when (not= quote-char " ")
      quote-char)))

(defn- get-db-metadata* [^Connection c]
  (let [metadata (.getMetaData c)]
    {:tables (get-table-metadata metadata c)
     :quote  (get-identifier-quote metadata)}))

(defn- get-db-metadata [datasource]
  (cond
    (instance? java.sql.Connection datasource)
    (get-db-metadata* datasource)

    (next.jdbc.default-options/wrapped-connection? datasource)
    (get-db-metadata* (:connectable datasource))

    :else
    (with-open [conn (jdbc/get-connection datasource)]
      (get-db-metadata* conn))))

(defn- unquote-table-name [quote-char table-name]
  ;; Remove db quote around the schema and table name; ex.: "\"schema\".\"table\""
  (if quote-char
    (let [re (->> (Pattern/quote quote-char)
                  (format "^%1$s|%1$s(?=\\.)|(?<=\\.)%1$s|%1$s$")
                  (re-pattern))]
      (str/replace table-name re ""))
    table-name))

(defn- metadata-matches-table?
  [^String table-name {:keys [table_schem table_name]}]
  (.equalsIgnoreCase table-name
                     (if (.contains table-name ".")
                       (str table_schem "." table_name)
                       table_name)))

(defn- table-exists? [datasource ^String table-name]
  (let [{:keys [tables quote]} (get-db-metadata datasource)
        table-name-unquoted    (unquote-table-name quote table-name)]
    (some (partial metadata-matches-table? table-name-unquoted)
          tables)))

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
