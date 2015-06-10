(defproject ragtime "0.3.9"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.3.9"]
                 [ragtime/ragtime.jdbc "0.3.9"]
                 [ragtime/ragtime.sql.files "0.3.9"]]
  :plugins [[lein-sub "0.2.1"]
            [codox "0.8.10"]]
  :sub ["ragtime.core" "ragtime.jdbc" "ragtime.sql.files"]
  :codox {:sources ["ragtime.core/src"
                    "ragtime.jdbc/src"
                    "ragtime.sql.files/src"]
          :exclude [ragtime.jdbc ragtime.main]})
