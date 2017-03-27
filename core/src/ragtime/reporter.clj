(ns ragtime.reporter
  (:refer-clojure :exclude [print]))

(defn silent
  "A reporter function that ignores the migration."
  [_ _ _])

(defn print
  "A reporter function that prints the migration ID to STDOUT."
  [_ op id]
  (case op
    :up   (println "Applying" id)
    :down (println "Rolling back" id)))
