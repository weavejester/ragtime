(defproject ragtime "0.3.0-SNAPSHOT"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.3.0-SNAPSHOT"]
                 [ragtime/ragtime.sql "0.3.0-SNAPSHOT"]
                 [ragtime/ragtime.sql.files "0.3.0-SNAPSHOT"]]
  :plugins [[lein-sub "0.2.0"]]
  :sub ["ragtime.core" "ragtime.sql" "ragtime.sql.files"])
