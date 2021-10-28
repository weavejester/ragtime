(defproject dev.weavejester/ragtime "0.8.1"
  :description "A database-independent migration library"
  :url "https://github.com/weavejester/ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[dev.weavejester/ragtime.core "0.8.1"]
                 [dev.weavejester/ragtime.jdbc "0.8.1"]
                 [dev.weavejester/ragtime.next-jdbc "0.8.1"]]
  :plugins [[lein-sub "0.3.0"]
            [lein-codox "0.10.3"]]
  :sub ["core" "jdbc" "next.jdbc"]
  :codox {:source-paths ["core/src" "jdbc/src"]
          :output-path  "codox"})
