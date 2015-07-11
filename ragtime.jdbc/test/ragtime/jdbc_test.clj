(ns ragtime.jdbc-test
  (:require [clojure.test :refer :all]
            [ragtime.jdbc :as jdbc]
            [ragtime.core :as core]
            [ragtime.protocols :as p]
            [clojure.java.jdbc :as sql]))

(deftest test-add-migrations
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1"})]
    (p/add-migration-id db "12")
    (p/add-migration-id db "13")
    (p/add-migration-id db "20")
    (is (= ["12" "13" "20"] (p/applied-migration-ids db)))
    (p/remove-migration-id db "13")
    (is (= ["12" "20"] (p/applied-migration-ids db)))))

(deftest test-migrations-table
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test2;DB_CLOSE_DELAY=-1"}
                         {:migrations-table "migrations"})]
    (p/add-migration-id db "12")
    (is (= ["12"]
           (sql/query (:db-spec db) ["SELECT * FROM migrations"] :row-fn :id)))))

(defn table-names [db]
  (set (sql/query (:db-spec db) ["SHOW TABLES"] :row-fn :table_name)))

(deftest test-sql-migration
  (let [db (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test3;DB_CLOSE_DELAY=-1"})
        m  (jdbc/sql-migration {:id   "01"
                                :up   ["CREATE TABLE foo (id int)"]
                                :down ["DROP TABLE foo"]})]
    (core/migrate db m)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO"} (table-names db)))
    (core/rollback db m)
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))))

(deftest test-load-directory
  (let [db  (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test4;DB_CLOSE_DELAY=-1"})
        ms  (jdbc/load-directory "test/migrations")
        idx (core/into-index ms)]
    (core/migrate-all db idx ms)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO" "BAR" "BAZ" "QUZA" "QUZB" "QUXA" "QUXB"}
           (table-names db)))
    (is (= ["001-test" "002-bar" "003-test" "004-test" "005-test"]
           (p/applied-migration-ids db)))
    (core/rollback-last db idx (count ms))
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))
    (is (empty? (p/applied-migration-ids db)))))

(deftest test-load-resources
  (let [db  (jdbc/sql-database {:connection-uri "jdbc:h2:mem:test5;DB_CLOSE_DELAY=-1"})
        ms  (jdbc/load-resources "migrations")
        idx (core/into-index ms)]
    (core/migrate-all db idx ms)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO" "BAR" "BAZ" "QUZA" "QUZB" "QUXA" "QUXB"}
           (table-names db)))
    (is (= ["001-test" "002-bar" "003-test" "004-test" "005-test"]
           (p/applied-migration-ids db)))
    (core/rollback-last db idx (count ms))
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))
    (is (empty? (p/applied-migration-ids db)))))
