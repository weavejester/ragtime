(defproject ragtime "0.2.1-SNAPSHOT"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.2.1-SNAPSHOT"]
                 [ragtime/ragtime.sql "0.2.1-SNAPSHOT"]]
  :dev-dependencies [[lein-sub "0.1.1"]]
  :sub ["ragtime.core" "ragtime.sql"])
