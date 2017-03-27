(ns ragtime.repl
  "Convenience functions for running in the REPL."
  (:require [ragtime.core :as core]
            [ragtime.protocols :as p]
            [ragtime.strategy :as strategy]
            [ragtime.reporter :as reporter]))

(def migration-index
  "An atom holding a map that matches migration IDs to known migrations. This
  atom is updated automatically when the migrate or rollback functions are
  called."
  (atom {}))

(defn- record-migrations [migrations]
  (swap! migration-index core/into-index migrations))

(defn migrate
  "Migrate the datastore up to the latest migration. Expects a configuration map
  with the following keys:

    :datastore  - a DataStore instance
    :migrations - an ordered collection of Migrations
    :strategy   - the conflict strategy to use
                  (defaults to ragtime.strategy/raise-error)
    :reporter   - called when a migration is being applied
                  (defaults to default-reporter)"
  [{:keys [datastore migrations strategy reporter]
    :or   {reporter reporter/print
           strategy strategy/raise-error}}]
  (let [index (record-migrations migrations)]
    (core/migrate-all datastore index migrations strategy reporter)))

(defn rollback
  "Rollback the datastore one or more migrations. Expects a configuration map
  and an optional number of migrations to roll back OR a migration ID to
  rollback to. The configuration expects the following keys:

    :datastore  - a DataStore instance
    :migrations - an ordered collection of Migrations
    :reporter   - called when a migration is being applied
                  (defaults to default-reporter) "
  ([config]
   (rollback config 1))
  ([{:keys [datastore migrations reporter]
     :or   {reporter reporter/print}}
    amount-or-id]
   (let [index (record-migrations migrations)]
     (if (integer? amount-or-id)
       (core/rollback-last datastore index amount-or-id reporter)
       (core/rollback-to datastore index amount-or-id reporter)))))
