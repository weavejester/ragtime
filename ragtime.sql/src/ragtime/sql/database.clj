(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable connection)])
  (:require [clojure.java.io :as io])
  (:import java.io.FileNotFoundException
           java.util.Date
           java.text.SimpleDateFormat))

(defn require-jdbc [ns-alias]
  (try
    (require 'clojure.java.jdbc.deprecated)
    (alias ns-alias 'clojure.java.jdbc.deprecated)
    (catch FileNotFoundException ex
      (require 'clojure.java.jdbc)
      (alias ns-alias 'clojure.java.jdbc))))

(require-jdbc 'sql)

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (sql/with-connection db
    (try
      (sql/create-table (:migrations-table db)
                        [:id "varchar(255)"]
                        [:created_at "varchar(32)"])
      (catch Exception _))))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/insert-values (:migrations-table db)
                         [:id :created_at]
                         [(str id) (format-datetime (Date.))])))
  
  (remove-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/delete-rows (:migrations-table db) ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/with-query-results results
        [(str "SELECT id FROM " (:migrations-table db) " ORDER BY created_at")]
        (vec (map :id results))))))


(def ^:private migrations-table "ragtime_migrations")

(defmethod connection "jdbc" [opts]
  (map->SqlDatabase {:connection-uri (:database opts)
                      :migrations-table (or (:table opts) migrations-table)}))
