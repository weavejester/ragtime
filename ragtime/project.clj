(defproject ragtime "0.4.0-SNAPSHOT"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.4.0-SNAPSHOT"]
                 [ragtime/ragtime.jdbc "0.4.0-SNAPSHOT"]]
  :plugins [[lein-sub "0.2.1"]
            [codox "0.8.10"]]
  :sub ["ragtime.core" "ragtime.jdbc"]
  :codox {:sources ["ragtime.core/src"
                    "ragtime.jdbc/src"]})
