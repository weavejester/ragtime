(ns leiningen.migrate
  (:use [leinjacker.eval :only (eval-in-project)])
  (:require [leinjacker.deps :as deps]))

(defn load-namespaces
  "Create require forms for each of the supplied symbols. This exists because
  Clojure cannot load and use a new namespace in the same eval form."
  [& syms]
  `(require
    ~@(for [s syms :when s]
        `'~(if-let [ns (namespace s)]
             (symbol ns)
             s))))

(defn migrate
  "Migrate the project database to the latest version."
  [project & args]
  (let [migrations (-> project :ragtime :migrations)
        database   (-> project :ragtime :database)]
    (eval-in-project
     (-> project
         (deps/add-if-missing '[ragtime/ragtime.core "0.3.0-SNAPSHOT"])
         (deps/add-if-missing '[ragtime/ragtime.sql "0.3.0-SNAPSHOT"]))
     `(ragtime.core/migrate-all
       (ragtime.core/connection ~database)
       (~migrations))
     (load-namespaces
      'ragtime.core
      'ragtime.sql.database
      migrations))))
