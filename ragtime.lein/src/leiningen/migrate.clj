(ns leiningen.migrate
  (:require [leinjacker.deps :as deps])
  (:use [leiningen.run :only (run)]))

(defn- add-ragtime-deps [project]
  (-> project
      (deps/add-if-missing '[ragtime/ragtime.core "0.3.0-SNAPSHOT"])
      (deps/add-if-missing '[ragtime/ragtime.sql "0.3.0-SNAPSHOT"])))

(defn migrate
  "Migrate the project database to the latest version."
  [project & args]
  (let [migrations (-> project :ragtime :migrations str)
        database   (-> project :ragtime :database)
        project    (add-ragtime-deps project)]
    (run project "-m" "ragtime.main" "-r" "ragtime.sql.database" database migrations)))
