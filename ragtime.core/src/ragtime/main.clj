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

(defn cli-main [database-url migrations-fn options]
  (doseq [ns (parse-namespaces options)]
    (require ns))
  (core/migrate-all
   (core/connection database-url)
   (map verbose-migration
        ((load-var migrations-fn)))))

(def ^:private cli-options
  ["-r" "--require" "Comma-separated list of namespaces to require"])

(defn -main
  "Migrates a database to the latest version when supplied with a database URL
  and a function that returns a sequence of migrations."
  [& args]
  (let [[opts args banner] (cli args cli-options)]
    (when (or (:help opts) (empty? args))
      (println banner)
      (System/exit 0))
    (apply cli-main (conj args opts))))
