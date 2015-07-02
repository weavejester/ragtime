(ns ragtime.repl
  "Convenience functions for running in the REPL."
  (:require [ragtime.core :as core]
            [ragtime.protocols :as p]
            [ragtime.strategy :as strategy]))

(def migration-index
  "An atom holding a map that matches migration IDs to known migrations. This
  atom is updated automatically when the migrate or rollback functions are
  called."
  (atom {}))

(defn default-reporter
  "A reporter function that just prints the migration ID to STDOUT."
  [op id]
  (case op
    :up   (println "Applying" id)
    :down (println "Rolling back" id)))

(defn ^:internal ^:no-doc record-migrations [migrations]
  (swap! migration-index core/into-index migrations))

(defn ^:internal ^:no-doc wrap-reporting [migration reporter]
  (let [id (p/id migration)]
    (reify p/Migration
      (id [_] id)
      (run-up! [_ store]
        (reporter :up id)
        (p/run-up! migration store))
      (run-down! [_ store]
        (reporter :down id)
        (p/run-down! migration store)))))

(defn migrate
  "Migrate the database up to the latest migration. Expects a configuration map
  with the following keys:

    :database  - a Migratable database
    :migration - an ordered collection of migrations
    :strategy  - the conflict strategy to use
                 (defaults to ragtime.strategy/raise-error)
    :reporter  - called when a migration is being applied
                 (defaults to default-reporter)"
  [{:keys [database migrations strategy reporter]
    :or   {reporter default-reporter
           strategy strategy/raise-error}}]
  (let [migrations (map #(wrap-reporting % reporter) migrations)
        index      (record-migrations migrations)]
    (core/migrate-all database index migrations strategy)))

(defn rollback
  "Rollback the database one or more migrations. Expects a configuration map
  and an optional number of migrations to roll back. The configuration expects
  the following keys:

    :database  - a Migratable database
    :migration - an ordered collection of migrations
    :reporter  - called when a migration is being applied
                 (defaults to default-reporter) "
  ([config]
   (rollback config 1))
  ([{:keys [database migrations reporter] :or {reporter default-reporter}} n]
   (let [migrations (map #(wrap-reporting % reporter) migrations)
         index      (record-migrations migrations)]
     (core/rollback-last database index n))))
