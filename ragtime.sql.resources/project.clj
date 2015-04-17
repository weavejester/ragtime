(defproject ragtime/ragtime.sql.resources "0.3.8"
  :description "Ragtime adapter that reads migrations from SQL resources."
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [ragtime/ragtime.sql "0.3.8"]
                 [ragtime/ragtime.sql.files "0.3.8"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}
   :test {:resource-paths ["test/resources"]}
   :java-jdbc-0.3.x [:dev {:dependencies [[org.clojure/java.jdbc "0.3.2"]]}]})
