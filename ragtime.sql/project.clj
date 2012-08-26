(defproject ragtime/ragtime.sql "0.3.0-SNAPSHOT"
  :description "Ragtime migrations for SQL databases"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [ragtime/ragtime.core "0.3.0-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.1.1"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}})
