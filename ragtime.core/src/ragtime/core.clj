(ns ragtime.core
  "Functions and macros for defining and applying migrations."
  (:use ragtime.database)
  (:require [ragtime.strategy :as strategy]))

(defonce defined-migrations (atom {}))

(defn remember-migration
  "Remember a migration so that it can be found from its ID. Automatically
  called by the defmigration macro and migrate function."
  [migration]
  (swap! defined-migrations assoc (:id migration) migration))

(defonce current-order (atom 0))

(defn next-order
  "Return an integer representing the order in which a migration was defined.
  Each successive call to this function will increment the order by 1."
  []
  (swap! current-order inc))

(defmacro defmigration
  "Defines a migration in the current namespace. The name of the migration must
  be unique. The migration itself is a map consisting of a :up function for
  applying the migration, and a :down function to rollback the migration.

  (defmigration some-migration
    {:up   #(apply-migration db)
     :down #(rollback-migration db)})"
  [name & [migration]]
  `(let [id# (str (ns-name *ns*) "/" ~(str name))]
     (def ~(with-meta name {:migration true})
       (assoc ~migration :id id# :order (next-order)))
     (remember-migration ~name)))

(defn- get-namespace [ns]
  (if (instance? clojure.lang.Namespace ns)
    ns
    (find-ns (symbol ns))))

(defn list-migrations
  "Lists the migrations in a namespace in the order in which they were defined.
  The namespace may be a symbol or a namespace object. If no argument is
  specified, the current namespace (*ns*) is used."
  ([]
     (list-migrations *ns*))
  ([namespace]
     (->> (get-namespace namespace)
          (ns-publics)
          (vals)
          (filter (comp :migration meta))
          (map var-get)
          (sort-by :order))))

(defn applied-migrations
  "List all migrations applied to the database."
  [db]
  (->> (applied-migration-ids db)
       (map @defined-migrations)))

(defn migrate
  "Apply a single migration to a database."
  [db migration]
  (remember-migration migration)
  ((:up migration))
  (add-migration-id db (:id migration)))

(defn rollback
  "Rollback a migration already applied to the database."
  [db migration]
  ((:down migration))
  (remove-migration-id db (:id migration)))

(defn migrate-all
  "Migrate all migrations using the supplied strategy. The strategy defines
  what to do if there are conflicts between the migrations applied to the
  database, and the migrations that need to be applied. The default
  strategy is ragtime.strategy/raise-error."
  ([db migrations]
     (migrate-all db migrations strategy/raise-error))
  ([db migrations strategy]
     (let [applied  (applied-migrations db)]
       (doseq [[action migration] (strategy applied migrations)]
         (case action
           :migrate  (migrate db migration)
           :rollback (rollback db migration))))))

(defn migrate-ns
  "Migrate all migrations in a namespace using the supplied strategy. Works
  like migrate-all."
  ([db namespace]
     (migrate-all db (list-migrations namespace)))
  ([db namespace strategy]
     (migrate-all db (list-migrations namespace) strategy)))
