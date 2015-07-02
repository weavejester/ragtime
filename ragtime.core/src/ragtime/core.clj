(ns ragtime.core
  "Functions for applying and rolling back migrations."
  (:require [ragtime.protocols :as p]
            [ragtime.strategy :as strategy]))

(defn into-index
  "Add migrations to a map, using their :id as the key."
  ([migrations]
   (into-index {} migrations))
  ([index migrations]
   (into index (map (juxt p/id identity) migrations))))

(defn applied-migrations
  "List all migrations in the index that are applied to the database."
  [store index]
  (->> (p/applied-migration-ids store)
       (map index)
       (remove nil?)))

(defn migrate
  "Apply a single migration to a data store."
  [store migration]
  (p/run-up! migration store)
  (p/add-migration-id store (p/id migration)))

(defn rollback
  "Rollback a migration already applied to the database."
  [store migration]
  (p/run-down! migration store)
  (p/remove-migration-id store (p/id migration)))

(defn migrate-all
  "Migrate a data store with the supplied index, migrations and
  strategy. The index matches IDs in the data store with their
  associated migrations. The strategy defines what to do if there are
  conflicts between the migrations applied to the data store, and the
  migrations that need to be applied. The default strategy is
  ragtime.strategy/raise-error."
  ([store index migrations]
   (migrate-all store index migrations strategy/raise-error))
  ([store index migrations strategy]
   (let [applied (applied-migrations store (into-index index migrations))]
     (doseq [[action migration] (strategy applied migrations)]
       (case action
         :migrate  (migrate store migration)
         :rollback (rollback store migration))))))

(defn rollback-last
  "Rollback the last n migrations from the database, using the supplied
  migration index. If n is not specified, only the very last migration is
  rolled back."
  ([store index]
   (rollback-last store index 1))
  ([store index n]
   (doseq [migration (take n (reverse (applied-migrations store index)))]
     (rollback store migration))))

(defn rollback-to
  "Rollback to a specific migration ID, using the supplied migration index."
  [store index migration-id]
  (let [migrations (applied-migrations store index)
        discards   (->> (reverse migrations)
                        (take-while #(not= (p/id %) migration-id)))]
    (if (= discards migrations)
      (throw (Exception. (str "Could not find migration '" migration-id "' in database")))
      (doseq [migration discards]
        (rollback store migration)))))
