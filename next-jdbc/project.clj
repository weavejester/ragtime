(defproject dev.weavejester/ragtime.next-jdbc "0.10.1"
  :description "Ragtime migrations for next.jdbc"
  :url "https://github.com/weavejester/ragtime"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.seancorfield/next.jdbc "1.3.955"]
                 [dev.weavejester/ragtime.core "0.10.1"]
                 [dev.weavejester/ragtime.sql "0.10.1"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "2.2.224"]]}})
