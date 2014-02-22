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

(def ^:private migrations-table "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (try
    (sql/execute! db [(format "CREATE TABLE %s (id varchar(255), created_at varchar(32))" migrations-table)])
    (catch Exception _)))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (ensure-migrations-table-exists db)
    (apply sql/insert! db :ragtime_migrations
           [[:id :created_at]
            [(str id) (format-datetime (Date.))]]))

  (remove-migration-id [db id]
    (ensure-migrations-table-exists db)
    (apply sql/delete! db :ragtime_migrations [["id = ?" id]]))

  (applied-migration-ids [db]
    (ensure-migrations-table-exists db)
    (vec (map :id (sql/query db ["SELECT id FROM ragtime_migrations ORDER BY created_at"])))))

(defmethod connection "jdbc" [url]
  (map->SqlDatabase {:connection-uri url}))
