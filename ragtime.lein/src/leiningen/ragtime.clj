(ns leiningen.ragtime
  (:require [leinjacker.deps :as deps])
  (:use [leiningen.run :only (run)]))

(defn- add-ragtime-deps [project]
  (-> project
      (deps/add-if-missing '[ragtime/ragtime.core "0.3.8"])
      (deps/add-if-missing '[ragtime/ragtime.sql "0.3.8"])))

(defn ragtime
  "Run ragtime.main with the options specified in the project file."
  [project command & args]
  (let [migrations (-> project :ragtime :migrations str)
        database   (-> project :ragtime :database)
        project    (add-ragtime-deps project)]
    (when-let [db (if (coll? database) database [database])]
      (apply run project
             "-m" "ragtime.main"
             "-r" "ragtime.sql.database"
             "-d" db
             "-m" migrations
             command args))))
