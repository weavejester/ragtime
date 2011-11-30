(ns ragtime.core
  "Functions and macros for defining and applying migrations."
  (:use [clojure.core.incubator :only (-?>)]
        [clojure.tools.macro :only (name-with-attributes)]
        ragtime.database)
  (:require [ragtime.strategy :as strategy]))

(defonce defined-migrations (atom {}))

(defn remember-migration [migration]
  (swap! defined-migrations assoc (:id migration) migration))

(defmacro defmigration
  "Defines a migration in the current namespace. The name of the migration must
  be unique. The migration itself is a map consisting of a :up function for
  applying the migration, and a :down function to rollback the migration.

  (defmigration some-migration
    {:up   #(apply-migration db)
     :down #(rollback-migration db)})"
  [name & [migration]]
  `(let [id# (str (ns-name *ns*) "/" ~(str name))]
     (def ~name (assoc ~migration :id id#))
     (defonce ~'db-migrations (atom []))
     (swap! ~'db-migrations conj ~name)
     (remember-migration ~name)))

(defn- namespace-name [ns]
  (if (instance? clojure.lang.Namespace ns)
    (name (ns-name ns))
    (name ns)))

(defn- migrations-ref [namespace]
  (-?> (namespace-name namespace)
       (symbol "db-migrations")
       (find-var)
       (var-get)))

(defn list-migrations
  "Lists the migrations in a namespace in the order in which they were defined."
  [namespace]
  (-?> (migrations-ref namespace)
       (deref)))

(defn reset-migrations!
  "Clears the migrations from an existing namespace. This should be evaluated
  at the beginning of any namespace holding migrations, so that reloads don't
  contain old migrations."
  [namespace]
  (-?> (migrations-ref namespace)
       (reset! [])))

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
