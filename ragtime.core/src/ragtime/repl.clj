(ns ragtime.repl
  "Convenience functions for running in the REPL."
  (:require [ragtime.core :as core]
            [ragtime.strategy :as strategy]))

(def migration-index
  "Index matching migration IDs to known migrations."
  (atom {}))

(defn migrate
  "Migrate the database up to the latest migration."
  [{:keys [database migrations strategy]}]
  (let [index (swap! migration-index core/into-index migrations)]
    (core/migrate-all database index migrations (or strategy strategy/raise-error))))

(defn rollback
  "Rollback the database one or more migrations."
  ([config]
   (rollback config 1))
  ([{:keys [database migrations]} n]
   (let [index (swap! migration-index core/into-index migrations)]
     (core/rollback-last database index n))))
