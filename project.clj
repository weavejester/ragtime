(defproject ragtime "0.3.8"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.3.8"]
                 [ragtime/ragtime.sql "0.3.8"]
                 [ragtime/ragtime.sql.files "0.3.8"]
                 [ragtime/ragtime.sql.resources "0.3.8"]]
  :plugins [[lein-sub "0.2.1"]
            [codox "0.8.10"]]
  :sub ["ragtime.core" "ragtime.sql" "ragtime.sql.files" "ragtime.sql.resources"]
  :codox {:sources ["ragtime.core/src"
                    "ragtime.sql/src"
                    "ragtime.sql.files/src"
                    "ragtime.sql.resources/src"]
          :exclude [ragtime.sql.database ragtime.main]})
