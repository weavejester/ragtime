(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable connection)])
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io])
  (:import (java.util Date)
           (java.sql Timestamp)))

(def ^:private migrations-table "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (sql/with-connection db
    (try
      (sql/create-table migrations-table
                        [:id "varchar(255)"]
                        [:created_at "timestamp"])
      (catch Exception _))))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/insert-values migrations-table
                         [:id :created_at] [(str id) (Timestamp. (.getTime (Date.)))])))

  (remove-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/delete-rows migrations-table ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/with-query-results results
        ["SELECT id FROM ragtime_migrations ORDER BY created_at"]
        (vec (map :id results))))))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
