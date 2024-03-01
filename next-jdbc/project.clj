(defproject dev.weavejester/ragtime.next-jdbc "0.9.3"
  :description "Ragtime migrations for next.jdbc"
  :url "https://github.com/weavejester/ragtime"
  :scm {:dir ".."}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [dev.weavejester/ragtime.core "0.9.3"]
                 [resauce "0.2.0"]]
  :profiles
  {:dev {:dependencies [[com.h2database/h2 "2.2.224"]]}})
