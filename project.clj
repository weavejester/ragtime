(defproject ragtime "0.4.0-SNAPSHOT"
  :description "A database-independent migration library"
  :url "https://github.com/weavejester/ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ragtime/ragtime.core "0.4.0-SNAPSHOT"]
                 [ragtime/ragtime.jdbc "0.4.0-SNAPSHOT"]]
  :plugins [[lein-sub "0.3.0"]
            [codox "0.8.12"]]
  :sub ["ragtime.core" "ragtime.jdbc"]
  :codox {:sources ["ragtime.core/src"
                    "ragtime.jdbc/src"]})
