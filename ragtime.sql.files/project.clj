(defproject ragtime/ragtime.sql.files "0.3.9-SNAPSHOT"
  :description "Ragtime adapter that reads migrations from SQL files."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ragtime/ragtime.sql "0.3.9-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.2.3"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}
   :java-jdbc-0.3.x [:dev {:dependencies [[org.clojure/java.jdbc "0.3.2"]]}]})
