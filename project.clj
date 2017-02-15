(defproject ragtime "0.6.4"
  :description "A database-independent migration library"
  :url "https://github.com/weavejester/ragtime"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ragtime/ragtime.core "0.6.4"]
                 [ragtime/ragtime.jdbc "0.6.4"]]
  :plugins [[lein-sub "0.3.0"]
            [lein-codox "0.9.5"]]
  :sub ["ragtime.core" "ragtime.jdbc"]
  :codox {:source-paths ["ragtime.core/src" "ragtime.jdbc/src"]
          :output-path  "codox"})
