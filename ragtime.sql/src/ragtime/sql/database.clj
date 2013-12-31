(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable connection)])
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.io :as io])
  (:import java.util.Date
           java.text.SimpleDateFormat))

(def ^:private migrations-table "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (let [ddl (jdbc/create-table-ddl migrations-table 
                                   [:id "varchar(255)"]
                                   [:created_at "varchar(32)"])]
    (try
      (jdbc/execute! db [ddl])
      (catch Exception _))))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (ensure-migrations-table-exists db)
    (jdbc/insert! db migrations-table {:id (str id)
                                       :created_at (format-datetime (Date.))}))
  
  (remove-migration-id [db id]
    (ensure-migrations-table-exists db)
    (jdbc/delete! db migrations-table ["id = ?" id]))

  (applied-migration-ids [db]
    (ensure-migrations-table-exists db)
    (jdbc/query db
      ["SELECT id FROM ragtime_migrations ORDER BY created_at"]
      :row-fn :id)))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
