(ns ragtime.core
  "Functions and macros for defining and applying migrations."
  (:require [ragtime.strategy :as strategy])
  (:import java.net.URI))

(defprotocol Migratable
  "Protocol for a database that can be migrated."
  (add-migration-id [db migration-id]
    "Add an applied migration ID to the database.")
  (remove-migration-id [db migration-id]
    "Remove a rolled-back migration ID from the database.")
  (applied-migration-ids [db]
    "Return a list of the ids of all migrations applied to the database."))

(defmulti connection
  "Create a Migratable database connection from a URL. Dispatches on the URL
  scheme."
  (fn [url]
    (.getScheme (URI. url))))

(defonce defined-migrations (atom {}))

(defn remember-migration
  "Remember a migration so that it can be found from its ID."
  [migration]
  (swap! defined-migrations assoc (:id migration) migration))

(defn applied-migrations
  "List all migrations applied to the database."
  [db]
  (remove nil? (->> (applied-migration-ids db)
                    (map @defined-migrations))))

(defn migrate
  "Apply a single migration to a database."
  [db migration]
  (remember-migration migration)
  ((:up migration) db)
  (add-migration-id db (:id migration)))

(defn rollback
  "Rollback a migration already applied to the database."
  [db migration]
  ((:down migration) db)
  (remove-migration-id db (:id migration)))

(defn migrate-all
  "Migrate all migrations using the supplied strategy. The strategy defines
  what to do if there are conflicts between the migrations applied to the
  database, and the migrations that need to be applied. The default
  strategy is ragtime.strategy/raise-error."
  ([db migrations]
     (migrate-all db migrations strategy/raise-error))
  ([db migrations strategy]
     (doseq [migration migrations]
       (remember-migration migration))
     (let [applied  (applied-migrations db)]
       (doseq [[action migration] (strategy applied migrations)]
         (case action
           :migrate  (migrate db migration)
           :rollback (rollback db migration))))))

(defn rollback-last
  "Rollback the last n previous migrations from the database. If n is not
  specified, only the very last migration is rolled back."
  ([db]
     (rollback-last db 1))
  ([db n]
     (doseq [migration (take n (reverse (applied-migrations db)))]
       (rollback db migration))))

(defn rollback-to
  "Rollback to a specific migration or migration ID."
  [db migration]
  (let [migrations   (applied-migrations db)
        migration-id (if (map? migration)
                       (:id migration)
                       migration)
        discards     (->> (reverse migrations)
                          (take-while #(not= (:id %) migration-id)))]
    (if (= discards migrations)
      (throw (Exception. (str "Could not find migration '" migration-id "'")))
      (doseq [migration discards]
        (rollback db migration)))))

(defn- wrap-println [f s]
  (fn [& args]
    (println s)
    (apply f args)))

(defn verbose-migration
  "Wraps a migration such that status messages are printed to stdout."
  [{:keys [id up down] :as migration}]
  (assoc migration
    :up   (wrap-println up   (str "Applying " id))
    :down (wrap-println down (str "Rolling back " id))))
