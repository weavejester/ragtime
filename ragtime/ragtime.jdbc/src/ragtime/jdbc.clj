(ns ragtime.jdbc
  (:require [ragtime.core :as ragtime]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql])
  (:import [java.util Date]
           [java.sql SQLException]
           [java.text SimpleDateFormat]))

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

(defn sql-database
  ([db-spec]
   (sql-database db-spec {}))
  ([db-spec {:keys [migrations-table] :or {migrations-table "ragtime_migrations"}}]
   (->SqlDatabase db-spec migrations-table)))
