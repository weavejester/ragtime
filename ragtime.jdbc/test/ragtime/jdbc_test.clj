(ns ragtime.jdbc-test
  (:require [clojure.test :refer :all]
            [ragtime.jdbc :as jdbc]
            [ragtime.core :as ragtime]
            [clojure.java.jdbc :as sql]))

(deftest test-add-migrations
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1"})]
    (ragtime/add-migration-id db "12")
    (ragtime/add-migration-id db "13")
    (ragtime/add-migration-id db "20")
    (is (= ["12" "13" "20"] (ragtime/applied-migration-ids db)))
    (ragtime/remove-migration-id db "13")
    (is (= ["12" "20"] (ragtime/applied-migration-ids db)))))

(deftest test-migrations-table
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1"}
                         {:migrations-table "migrations"})]
    (ragtime/add-migration-id db "12")
    (is (= ["12"]
           (sql/query (:db-spec db) ["SELECT * FROM migrations"] :row-fn :id)))))

(defn table-names [db]
  (set (sql/query (:db-spec db) ["SHOW TABLES"] :row-fn :table_name)))

(deftest test-sql-migration
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1"})
        m  (jdbc/sql-migration {:id   "01"
                                :up   ["CREATE TABLE foo (id int)"]
                                :down ["DROP TABLE foo"]})]
    (ragtime/migrate db m)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO"} (table-names db)))
    (ragtime/rollback db m)
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))))

(deftest test-load-directory
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test4;DB_CLOSE_DELAY=-1"})
        ms (jdbc/load-directory "test/migrations")]
    (ragtime/migrate-all db ms)
    (is (= #{"RAGTIME_MIGRATIONS" "TESTING"} (table-names db)))
    (ragtime/rollback-last db (count ms))
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))))
