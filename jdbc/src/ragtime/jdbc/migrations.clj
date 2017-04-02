(ns ragtime.jdbc.migrations
  (:require [clojure.java.jdbc :as jdbc]
            [ragtime.jdbc :as ragtime]))

(defn create-table [id table specs]
  (ragtime/sql-migration
   {:id   id
    :up   [(jdbc/create-table-ddl table specs)]
    :down [(jdbc/drop-table-ddl table)]}))
