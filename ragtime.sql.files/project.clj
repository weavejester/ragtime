(defproject ragtime/ragtime.sql.files "0.3.4"
  :description "Ragtime adapter that reads migrations from SQL files."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.3.0"]
                 [java-jdbc/dsl "0.1.0"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]
                        [ragtime/ragtime.sql "0.3.4"]]}})
