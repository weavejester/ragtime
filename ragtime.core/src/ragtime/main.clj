(ns ragtime.main
  "Migrate databases via the command line."
  (:use [clojure.tools.cli :only (cli)])
  (:require [ragtime.core :as core]
            [clojure.string :as str]))

(defn- load-var [var-name]
  (let [var-sym (symbol var-name)]
    (require (-> var-sym namespace symbol))
    (find-var var-sym)))

(defn parse-namespaces [{namespaces :require}]
  (->> (str/split namespaces #"\s*,\s*")
       (map symbol)))

(defn- wrap-println [f s]
  (fn [& args]
    (println s)
    (apply f args)))

(defn- verbose-migration [{:keys [id up down] :as migration}]
  (assoc migration
    :up   (wrap-println up   (str "Applying " id))
    :down (wrap-println down (str "Rolling back " id))))

(defn migrate [{:keys [database migrations]}]
  (core/migrate-all
   (core/connection database)
   (map verbose-migration
        ((load-var migrations)))))

(defn rollback [{:keys [database migrations]} & [n]]
  (let [db (core/connection database)]
    (doseq [m ((load-var migrations))]
      (core/remember-migration m))
    (core/rollback-last db (or n 1))))

(defn parse-args [args]
  (cli args
       ["-r" "--require" "Comma-separated list of namespaces to require"]
       ["-d" "--database" "Database URL"]
       ["-m" "--migrations" "A function that returns a list of migrations"]))

(defn -main
  "Migrates a database to the latest version when supplied with a database URL
  and a function that returns a sequence of migrations."
  [& args]
  (let [[options [command & args]] (parse-args args)]
    (doseq [ns (parse-namespaces options)]
      (require ns))
    (case command
      "migrate"  (apply migrate options args)
      "rollback" (apply rollback options args))))
