(ns ragtime.sql.database
  (:require [ragtime.core :as ragtime]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql])
  (:import java.io.FileNotFoundException
           java.util.Date
           java.text.SimpleDateFormat))

(def ^:private migrations-table "ragtime_migrations")

(def ^:private migrations-table-ddl
  (sql/create-table-ddl migrations-table
                        [:id "varchar(255)"]
                        [:created_at "varchar(32)"]))

(defn ensure-migrations-table-exists [db]
  (try
    (sql/execute! db migrations-table-ddl)
    (catch Exception _)))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  ragtime/Migratable
  (add-migration-id [db id]
    (ensure-migrations-table-exists db)
    (sql/insert! db migrations-table
                 {:id :created_at
                  (str id) (format-datetime (Date.))}))
  
  (remove-migration-id [db id]
    (ensure-migrations-table-exists db)
    (sql/delete! db migrations-table ["id = ?" id]))

  (applied-migration-ids [db]
    (ensure-migrations-table-exists db)
    (sql/query db
      ["SELECT id FROM ragtime_migrations ORDER BY created_at"]
      :row-fn :id)))

(defmethod ragtime/connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
