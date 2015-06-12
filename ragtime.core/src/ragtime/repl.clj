(ns ragtime.repl
  "Convenience functions for running in the REPL."
  (:require [ragtime.core :as core]
            [ragtime.strategy :as strategy]))

(defn migrate
  "Migrate the database up to the latest migration."
  [{:keys [database migrations strategy]}]
  (core/migrate-all database migrations (or strategy strategy/raise-error)))

(defn rollback
  "Rollback the database one or more migrations."
  ([config] (rollback config 1))
  ([{:keys [database]} n] (core/rollback-last database n)))
