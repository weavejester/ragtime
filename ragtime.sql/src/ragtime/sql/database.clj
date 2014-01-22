(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable connection)])
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io])
  (:import java.util.Date
           java.text.SimpleDateFormat))

(def ^:private migrations-table "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [conn]
  ;; TODO: is there a portable way to detect table existence?
  (try
    (sql/db-do-commands conn (sql/create-table-ddl migrations-table 
                                                    [:id "varchar(255)"]
                                                    [:created_at "varchar(32)"]))
    (catch Exception _)))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (sql/with-db-connection [conn db]
      (ensure-migrations-table-exists conn)
      (sql/insert! conn migrations-table
                   [:id :created_at]
                   [(str id) (format-datetime (Date.))])))

  (remove-migration-id [db id]
    (sql/with-db-connection [conn db]
      (ensure-migrations-table-exists conn)
      (sql/delete! conn migrations-table ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-db-connection [conn db]
      (ensure-migrations-table-exists conn)
      (map :id (sql/query conn
        ["SELECT id FROM ragtime_migrations ORDER BY created_at"]
        )))))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
