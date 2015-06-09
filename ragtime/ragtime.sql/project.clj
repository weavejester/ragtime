(defproject ragtime/ragtime.sql "0.4.0-SNAPSHOT"
  :description "Ragtime migrations for SQL databases"
  :url "https://github.com/weavejester/ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ragtime/ragtime.core "0.4.0-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.2.3"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}
   :java-jdbc-0.3.x [:dev {:dependencies [[org.clojure/java.jdbc "0.3.2"]]}]})
