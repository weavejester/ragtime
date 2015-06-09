(defproject ragtime/ragtime.sql "0.4.0-SNAPSHOT"
  :description "Ragtime migrations for SQL databases"
  :url "https://github.com/weavejester/ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [ragtime/ragtime.core "0.4.0-SNAPSHOT"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.3.160"]]}})
