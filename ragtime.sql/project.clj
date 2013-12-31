(defproject ragtime/ragtime.sql "0.3.4"
  :description "Ragtime migrations for SQL databases"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ragtime/ragtime.core "0.3.4"]
                 [org.clojure/java.jdbc "0.3.2"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}})
