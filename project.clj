(defproject ragtime "0.2.1"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.2.1"]
                 [ragtime/ragtime.sql "0.2.1"]]
  :plugins [[lein-sub "0.2.0"]]
  :sub ["ragtime.core" "ragtime.sql"])
