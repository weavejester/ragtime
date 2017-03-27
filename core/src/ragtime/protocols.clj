(ns ragtime.protocols)

(defprotocol Migration
  "Protocol for a migration that can be applied to a DataStore."
  (id [_]
    "Return the string identifier of the migration.")
  (run-up! [_ store]
    "Run the 'up' part of the migration.")
  (run-down! [_ store]
    "Run the 'down' part of the migration."))

(defprotocol DataStore
  "Protocol for a data store that can be migrated."
  (add-migration-id [store migration-id]
    "Add an applied migration ID to the data store.")
  (remove-migration-id [store migration-id]
    "Remove a rolled-back migration ID from the data store.")
  (applied-migration-ids [store]
    "Return an ordered list of ids of all migrations applied to the data store."))
