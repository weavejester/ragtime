(ns ragtime.sql.test.database
  (:use [clojure.test]
        [ragtime.sql.database]
        [ragtime.core :only [add-migration-id remove-migration-id
                             applied-migration-ids]])
  (:require [clojure.java.jdbc :as sql]))

(def test-db (ragtime.sql.database.SqlDatabase.
              "org.h2.Driver" "h2" "mem:test_db" "test" ""))

(defn h2-fixture [f]
  (sql/with-connection test-db
    (f)))

(use-fixtures :each h2-fixture)

(deftest test-add-migrations
  (add-migration-id test-db "12")
  (add-migration-id test-db "13")
  (add-migration-id test-db "20")
  (is (= ["12" "13" "20"] (applied-migration-ids test-db)))
  (remove-migration-id test-db "13")
  (is (= ["12" "20"] (applied-migration-ids test-db))))