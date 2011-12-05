(ns ragtime.sql.test.database
  (:use clojure.test
        ragtime.sql.database))

(def test-db
  {:classname "org.h2.Driver"
   :subprotocol "h2"
   :subname "mem:test_db"
   :user "test"
   :password ""})
