(defproject ragtime "0.3.7"
  :description "A database-independent migration library"
  :dependencies [[ragtime/ragtime.core "0.3.7"]
                 [ragtime/ragtime.sql "0.3.7"]
                 [ragtime/ragtime.sql.files "0.3.7"]]
  :plugins [[lein-sub "0.2.1"]
            [codox "0.6.7"]]
  :sub ["ragtime.core" "ragtime.sql" "ragtime.sql.files"]
  :codox {:sources ["ragtime.core/src"
                    "ragtime.sql/src"
                    "ragtime.sql.files/src"]
          :exclude [ragtime.sql.database ragtime.main]}
  :profiles
  {:dev {:dependencies [[org.clojure/clojure "1.6.0"]
                        [codox/codox.core "0.6.7"]]}})
