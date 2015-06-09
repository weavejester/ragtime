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

(def ^:dynamic *migrations-table* "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (sql/with-connection db
    (try
      (sql/create-table *migrations-table*
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
      (sql/insert-values *migrations-table*
                         [:id :created_at]
                         [(str id) (format-datetime (Date.))])))

  (remove-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/delete-rows *migrations-table* ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/with-query-results results
        [(format "SELECT id FROM %s ORDER BY created_at" *migrations-table*)]
        (vec (map :id results))))))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
