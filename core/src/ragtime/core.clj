(ns ragtime.core
  "Functions for applying and rolling back migrations."
  (:require [ragtime.protocols :as p]
            [ragtime.strategy :as strategy]
            [ragtime.reporter :as reporter]))

(extend-protocol p/MigrationIndex
  clojure.lang.IPersistentMap
  (index-migration [index key migration]
    (assoc index key migration))
  (deindex-migration [index key]
    (dissoc index key))
  (get-indexed-migration [index key not-found]
    (get index key not-found)))

(defn into-index
  "Add migrations to a map, using their ID as the key."
  ([migrations]
   (into-index {} migrations))
  ([index migrations]
   (reduce #(p/index-migration %1 (p/id %2) %2) index migrations)))

(defn applied-migrations
  "List all migrations in the index that are applied to the database."
  [store index]
  (keep #(p/get-indexed-migration index % nil)
        (p/applied-migration-ids store)))

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
  "Migrate a data store with the supplied index and migration sequence. The
  index matches IDs in the data store with their associated migrations. The
  migrations should be a sequential collection in the order in which they
  should be applied to the database.

  An additional map of options may be supplied that contains the following
  keys:

  :strategy - defines what to do if there are conflicts between the migrations
              applied to the data store, and the migrations that need to be
              applied. The default strategy is ragtime.strategy/raise-error.

  :reporter - a function that takes three arguments: the store, the operation
              (:up or :down) and the migration ID. The reporter is a
              side-effectful callback that can be used to print or report on
              the migrations as they are applied. The default reporter is
              ragtime.reporter/silent."
  ([store index migrations]
   (migrate-all store index migrations {}))
  ([store index migrations options]
   (let [strategy (:strategy options strategy/raise-error)
         reporter (:reporter options reporter/silent)
         index    (into-index index migrations)
         applied  (p/applied-migration-ids store)]
     (doseq [[action migration-id] (strategy applied (map p/id migrations))]
       (when (.isInterrupted (Thread/currentThread))
         (throw (InterruptedException.
                 (str "Stopping running migrations before " migration-id
                      " because the current thread has been interrupted"))))
       (if-some [migration (p/get-indexed-migration index migration-id nil)]
         (case action
           :migrate  (do (reporter store :up migration-id)
                         (migrate store migration))
           :rollback (do (reporter store :down migration-id)
                         (rollback store migration)))
         (throw (ex-info (str "Could not find migration '" migration-id
                              "' in index")
                         {:reason ::migration-not-indexed
                          :index index
                          :migration-id migration-id})))))))

(defn rollback-last
  "Rollback the last n migrations from the database, using the supplied
  migration index. If n is not specified, only the very last migration is
  rolled back.

  Takes an option map that may include the :reporter key. See migrate-all."
  ([store index]
   (rollback-last store index 1))
  ([store index n]
   (rollback-last store index n {}))
  ([store index n options]
   (let [reporter (:reporter options reporter/silent)]
     (doseq [migration (take n (reverse (applied-migrations store index)))]
       (reporter store :down (p/id migration))
       (rollback store migration)))))

(defn rollback-to
  "Rollback to a specific migration ID, using the supplied migration index.

  Takes an option map that may include the :reporter key. See migrate-all."
  ([store index migration-id]
   (rollback-to store index migration-id {}))
  ([store index migration-id options]
   (let [reporter   (:reporter options reporter/silent)
         migrations (applied-migrations store index)
         discards   (->> (reverse migrations)
                         (take-while #(not= (p/id %) migration-id)))]
     (if (= (count discards) (count migrations))
       (throw (ex-info (str "Could not find migration '" migration-id
                            "' in data store")
                       {:reason       ::migration-not-stored
                        :store        store
                        :migration-id migration-id}))
       (doseq [migration discards]
         (reporter store :down (p/id migration))
         (rollback store migration))))))
