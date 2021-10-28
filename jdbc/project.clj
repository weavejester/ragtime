(defproject ragtime/jdbc "0.8.1"
  :description "Ragtime migrations for JDBC"
  :url "https://github.com/weavejester/ragtime"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [ragtime/core "0.8.1"]
                 [resauce "0.2.0"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "1.4.200"]]}})
