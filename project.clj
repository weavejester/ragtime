(defproject dev.weavejester/ragtime "0.10.1"
  :description "A database-independent migration library"
  :url "https://github.com/weavejester/ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[dev.weavejester/ragtime.core "0.10.1"]
                 [dev.weavejester/ragtime.sql "0.10.1"]
                 [dev.weavejester/ragtime.jdbc "0.10.1"]
                 [dev.weavejester/ragtime.next-jdbc "0.10.1"]]
  :plugins [[lein-sub "0.3.0"]
            [lein-codox "0.10.8"]]
  :sub ["core" "sql" "jdbc" "next-jdbc"]
  :codox {:source-paths ["core/src" "sql/src" "jdbc/src" "next-jdbc/src"]
          :output-path  "codox"})
