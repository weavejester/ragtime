(ns ragtime.core
  "Functions and macros for defining and applying migrations."
  (:use [clojure.core.incubator :only (-?>)]
        [clojure.tools.macro :only (name-with-attributes)]
        ragtime.database)
  (:require [ragtime.strategy :as strategy]))

(defprotocol Migration
  "The migration protocol is meant to be reified into a single migration
  unit. See the defmigration macro."
  (id [migration] "A unique identifier for the migration")
  (up [migration] "Apply the migration to the database")
  (down [migration] "Rollback the migration"))

(defmacro migration
  "Creates a new migration with the supplied id and clauses. The id must be
  unique. The clauses consist of an :up form, which is evaluated when the
  migration is applied, and a :down form, which is evalidated when the migration
  is rolled back.

  (migration \"a-unique-id\"
     (:up (apply-migration))
     (:down (rollback-migration))"
  [id & clauses]
  (let [methods (reduce
                 (fn [m [k & body]] (assoc m k body))
                 {}
                 clauses)]
    `(reify Migration
       (id [_] (name ~id))
       (up [_] ~@(:up methods))
       (down [_] ~@(:down methods)))))

(defonce defined-migrations (atom {}))

(defn remember-migration [migration]
  (swap! defined-migrations assoc (id migration) migration))

(defmacro defmigration
  "Defines a migration in the current namespace. See ragtime.core/migration.

  (defmigration a-migration
    (:up (apply-migration))
    (:down (rollback-migration)))"
  [name & args]
  (let [[name args] (name-with-attributes name args)]
    `(let [id# (str (ns-name *ns*) "/" ~(str name))]
       (def ~name (migration id# ~@args))
       (defonce ~'db-migrations (atom []))
       (swap! ~'db-migrations conj ~name)
       (remember-migration ~name))))

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
  (up migration)
  (add-migration-id db (id migration)))

(defn rollback
  "Rollback a migration already applied to the database."
  [db migration]
  (down migration)
  (remove-migration-id db (id migration)))

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
