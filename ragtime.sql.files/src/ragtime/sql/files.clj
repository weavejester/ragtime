(ns ragtime.sql.files
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def ^:private migration-pattern
  #"(.*)\.(up|down)\.sql$")

(defn- migration? [file]
  (re-find migration-pattern (.getName (io/file file))))

(defn- migration-id [file]
  (second (re-find migration-pattern (.getName (io/file file)))))

(defn- get-migration-files [dir]
  (->> (.listFiles (io/file dir))
       (filter migration?)
       (sort)
       (group-by migration-id)))

(defn- run-sql-fn [file]
  (fn [db]
    (sql/with-connection db
      (sql/transaction
       (let [f (slurp file)]
         (prn f)
         (sql/do-commands f))))))

(defn- make-migration [[id [down up]]]
  {:id   id
   :up   (run-sql-fn up)
   :down (run-sql-fn down)})

(def ^:private default-dir "migrations")

(defn migrations
  "Return a list of migrations to apply."
  ([]    (migrations default-dir))
  ([dir] (map make-migration (get-migration-files dir))))
