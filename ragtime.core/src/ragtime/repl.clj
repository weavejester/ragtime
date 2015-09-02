(ns ragtime.repl
  "Convenience functions for running in the REPL."
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [ragtime.core :as core]
            [ragtime.protocols :as p]
            [ragtime.strategy :as strategy]))

(def date-format
  "Format for DateTime"
  "yyyyMMddHHmmss")
(def migrations-dir
  "Default migrations directory"
  "resources/migrations/")
(def ragtime-format-edn
  "EDN template for SQL migrations"
  "{:up [\"\"]\n :down [\"\"]}")

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

(defn migrations-dir-exist?
  "Checks if 'resources/migrations' directory exists"
  []
  (.isDirectory (io/file migrations-dir)))

(defn now
  "Gets the current DateTime"  []
  (.format (java.text.SimpleDateFormat. date-format) (new java.util.Date)))

(defn migration-file-path
  "Complete migration file path"
  [name]
  (str migrations-dir (now) "_" (s/replace name #"\s+|-+|_+" "_") ".edn"))

(defn create-migration
  "Creates a migration file with the current DateTime"
  [name]
  (let [migration-file (migration-file-path name)]
    (if-not (migrations-dir-exist?)
      (io/make-parents migration-file))
    (spit migration-file ragtime-format-edn)))

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
    :or   {reporter default-reporter
           strategy strategy/raise-error}}]
  (let [migrations (map #(wrap-reporting % reporter) migrations)
        index      (record-migrations migrations)]
    (core/migrate-all datastore index migrations strategy)))

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
  ([{:keys [datastore migrations reporter] :or {reporter default-reporter}}
    amount-or-id]
   (let [migrations (map #(wrap-reporting % reporter) migrations)
         index      (record-migrations migrations)]
     (if (integer? amount-or-id)
       (core/rollback-last datastore index amount-or-id)
       (core/rollback-to datastore index amount-or-id)))))
