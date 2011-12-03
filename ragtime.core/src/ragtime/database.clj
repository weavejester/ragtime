(ns ragtime.database
  "Defines a protocol for interfacing with a database.")

(defprotocol Database
  "Protocol that represents a database that migrations can be applied to."
  (add-migration-id [db migration-id]
    "Add an applied migration ID to the database.")
  (remove-migration-id [db migration-id]
    "Remove a rolled-back migration ID from the database.")
  (applied-migration-ids [db]
    "Return a list of the ids of all migrations applied to the database."))
