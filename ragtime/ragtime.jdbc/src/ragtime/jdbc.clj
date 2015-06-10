(ns ragtime.jdbc
  (:require [ragtime.core :as ragtime]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql])
  (:import java.util.Date
           java.sql.SQLException
           java.text.SimpleDateFormat))

(def ^:private migrations-table "ragtime_migrations")

(def ^:private migrations-table-ddl
  (sql/create-table-ddl migrations-table
                        [:id "varchar(255)"]
                        [:created_at "varchar(32)"]))

(defn ensure-migrations-table-exists [db-spec]
  (try
    (sql/execute! db-spec [migrations-table-ddl])
    (catch SQLException _)))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase [db-spec]
  ragtime/Migratable
  (add-migration-id [_ id]
    (ensure-migrations-table-exists db-spec)
    (sql/insert! db-spec migrations-table
                 {:id         (str id)
                  :created_at (format-datetime (Date.))}))
  
  (remove-migration-id [_ id]
    (ensure-migrations-table-exists db-spec)
    (sql/delete! db-spec migrations-table ["id = ?" id]))

  (applied-migration-ids [_]
    (ensure-migrations-table-exists db-spec)
    (sql/query db-spec
      ["SELECT id FROM ragtime_migrations ORDER BY created_at"]
      :row-fn :id)))

(defn sql-database [db-spec]
  (->SqlDatabase db-spec))
