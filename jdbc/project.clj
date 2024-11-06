(defproject dev.weavejester/ragtime.jdbc "0.9.5"
  :description "Ragtime migrations for JDBC"
  :url "https://github.com/weavejester/ragtime"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [dev.weavejester/ragtime.core "0.9.5"]
                 [resauce "0.2.0"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "2.2.224"]]}})
