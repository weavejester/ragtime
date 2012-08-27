(defproject ragtime "0.3.0-SNAPSHOT"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.lein "0.3.0-SNAPSHOT"]]
  :plugins [[lein-sub "0.2.0"]]
  :sub ["ragtime.core" "ragtime.sql" "ragtime.sql.files"])
