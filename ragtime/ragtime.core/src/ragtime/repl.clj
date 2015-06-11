(ns ragtime.repl
  "Convenience functions for running in the REPL."
  (:require [ragtime.core :as core]
            [ragtime.strategy :as strategy]))

(def config nil)

(defn set-config!
  "Set the REPL configuration. Expects a map with the following keys:
  
    :loader   - a zero argument function that returns a list of migrations
    :database - a Migratable database (e.g. ragtime.jdbc/sql-database)
    :strategy - the migration strategy (defaults to ragtime.strategy/raise-error)"
  [new-config]
  (alter-var-root #'config (constantly new-config)))

(def migrations nil)

(defn reload-migrations
  "Reload migrations using the :loader option in the configuration. This is
  called automatically by migrate and rollback."
  []
  (let [load-migrations (:loader config)]
    (alter-var-root #'migrations (fn [_] (load-migrations)))
    (apply core/remember-migration migrations)))

(defn migrate
  "Migrate the database up to the latest migration."
  []
  (reload-migrations)
  (core/migrate-all (:database config)
                    migrations
                    (:strategy config strategy/raise-error)))

(defn rollback
  "Rollback the database one or more migrations."
  ([] (rollback 1))
  ([n]
   (reload-migrations)
   (core/rollback-last (:database config) n)))
