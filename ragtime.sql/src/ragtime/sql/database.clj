(ns ragtime.sql.database
  (:use [ragtime.core :only [Migratable migrate-all]])
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.util Date)
           (java.net URI)))

(def ^{:private true} migrations-table "ragtime_migrations")

(defn- ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (sql/with-connection db
    (try
      (sql/create-table migrations-table
                        [:id "varchar(255)"]
                        [:created_at "datetime"])
      (catch Exception _))))

(defrecord SqlDatabase [host port path scheme user password subname classname]
  Migratable
  (add-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/insert-values migrations-table
                         [:id :created_at] [(str id) (Date.)])))
  
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

(defn make-database [map]
  ;; wait, srsly? this is how you do it?
  (into (SqlDatabase. nil nil nil nil nil nil nil nil) map))

(defn database-from-uri [uri]
  (if (instance? URI uri)
    ;; TODO: should this have been public?
    (make-database (#'clojure.java.jdbc.internal/parse-properties-uri uri))
    (database-from-uri (URI. uri))))

(def ^{:private true} migration-pattern #"(.*)\.(up|down)\.sql$")

(defn- migration? [file]
  (re-find migration-pattern (.getName (io/file file))))

(defn- migration-id [file]
  (second (re-find migration-pattern (.getName (io/file file)))))

(defn- get-migration-files [dir]
  (->> (.listFiles (io/file dir))
       (filter migration?)
       (sort)
       (group-by migration-id)))

(defn- run-sql-fn [file]
  (fn [db]
    (sql/with-connection db
      (sql/transaction
       (sql/do-commands (slurp file))))))

(defn- make-migration [[id [down up]]]
  {:id id
   :order (or (second (re-find #"^(\d+)" id)) id)
   :up (run-sql-fn up)
   :down (run-sql-fn down)})

(defn migrations [dir]
  (map make-migration (get-migration-files dir)))

;; TODO: this might not be the best way of invoking, but it works
;; nicely with lein run. Migrating shouldn't necessarily be a lein
;; plugin since it needs to be easy to make happen in production, not
;; just during development. So lein run may fit the bill?
(defn -main
  ([dir db] (migrate-all db (migrations dir)))
  ([dir] (-main dir (database-from-uri (System/getenv "DATABASE_URL"))))
  ;; TODO: can we make this work with files in jars?
  ([] (-main "migrations")))