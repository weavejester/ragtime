(ns ragtime.sql.test.resources
  (:require [clojure.string :as str])
  (:use clojure.test
        ragtime.sql.resources
        ragtime.sql.database
        ragtime.core))

(ragtime.sql.database/require-jdbc 'sql)

(def test-db
  (connection "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1"))

(defn table-exists? [table-name]
  (not-empty
   (sql/with-query-results rs
     ["select true from information_schema.tables where table_name = ?"
      (str/upper-case table-name)]
     (vec rs))))

(deftest test-migrations
  (let [mig-id "20111202110600-create-foo-table"
        migs ((migrations [mig-id]))]
    (is (= (count migs) 1))
    (is (= (:id (first migs)) mig-id))
    (migrate-all test-db migs)
    (sql/with-connection test-db
      (is (table-exists? "ragtime_migrations"))
      (is (table-exists? "foo")))))

(deftest test-incomplete-migrations
  (is (thrown-with-msg? AssertionError
                        #"Incomplete migrations"
                        ((migrations
                          ["migration1" "migration2"]
                          "incomplete-migrations")))))
