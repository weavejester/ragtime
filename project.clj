(defproject ragtime "0.1.1"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.1.1"]
                 [ragtime/ragtime.sql "0.1.1"]]
  :dev-dependencies [[lein-sub "0.1.1"]]
  :sub ["ragtime.core" "ragtime.sql"])
