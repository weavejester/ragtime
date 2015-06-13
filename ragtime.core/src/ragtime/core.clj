(ns ragtime.core
  "Functions and macros for defining and applying migrations."
  (:require [ragtime.strategy :as strategy]))

(defprotocol Migratable
  "Protocol for a database that can be migrated."
  (add-migration-id [db migration-id]
    "Add an applied migration ID to the database.")
  (remove-migration-id [db migration-id]
    "Remove a rolled-back migration ID from the database.")
  (applied-migration-ids [db]
    "Return a list of the ids of all migrations applied to the database."))

(defn into-index
  "Add migrations to a map, using their :id as the key."
  ([migrations]
   (into-index {} migrations))
  ([index migrations]
   (into index (map (juxt :id identity) migrations))))

(defn applied-migrations
  "List all migrations in the index that are applied to the database."
  [db index]
  (->> (applied-migration-ids db)
       (map index)
       (remove nil?)))

(defn migrate
  "Apply a single migration to a database."
  [db migration]
  ((:up migration) db)
  (add-migration-id db (:id migration)))

(defn rollback
  "Rollback a migration already applied to the database."
  [db migration]
  ((:down migration) db)
  (remove-migration-id db (:id migration)))

(defn migrate-all
  "Migrate a database with the supplied index, migrations and
  strategy. The index matches IDs in the database with their
  associated migrations. The strategy defines what to do if there are
  conflicts between the migrations applied to the database, and the
  migrations that need to be applied. The default strategy is
  ragtime.strategy/raise-error. "
  ([db index migrations]
   (migrate-all db index migrations strategy/raise-error))
  ([db index migrations strategy]
   (let [applied (applied-migrations db (into-index index migrations))]
     (doseq [[action migration] (strategy applied migrations)]
       (case action
         :migrate  (migrate db migration)
         :rollback (rollback db migration))))))

(defn rollback-last
  "Rollback the last n migrations from the database, using the supplied
  migration index. If n is not specified, only the very last migration is
  rolled back."
  ([db index]
   (rollback-last db index 1))
  ([db index n]
   (doseq [migration (take n (reverse (applied-migrations db index)))]
     (rollback db migration))))

(defn rollback-to
  "Rollback to a specific migration ID, using the supplied migration index."
  [db index id]
  (let [migrations (applied-migrations db index)
        discards   (->> (reverse migrations)
                        (take-while #(not= (:id %) id)))]
    (if (= discards migrations)
      (throw (Exception. (str "Could not find migration '" id "' in database")))
      (doseq [migration discards]
        (rollback db migration)))))
