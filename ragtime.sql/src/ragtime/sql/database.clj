(ns ragtime.sql.database
  (:use [ragtime.core :only (Migratable)])
  (:require [clojureql.core :as sql]))

(defn- create-migration-table [db])

(defn- migration-table-exists? [db] false)

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (if (migration-table-exists? db)
      (create-migration-table db))
    (sql/conj! (sql/table db migrations-table) {:id id}))
  
  (remove-migration-id [db id]
    (if (migration-table-exists? db)
      (sql/disj! (sql/table db migrations-table)
                 (where (= :id id)))))

  (applied-migration-ids [db]
    (if (migration-table-exists? db)
      @(sql/table db migrations-table))))
