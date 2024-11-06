(ns ragtime.next-jdbc-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [ragtime.next-jdbc :as jdbc]
            [ragtime.core :as core]
            [ragtime.protocols :as p]
            [clojure.java.io :as io]
            [next.jdbc :as n.j]
            [next.jdbc.sql :as sql]))

(def datasource {:jdbcUrl "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"})

(use-fixtures :each (fn reset-db [f]
                      (n.j/execute! datasource ["DROP ALL OBJECTS"])
                      (f)))

(deftest test-add-migrations
  (let [db (jdbc/sql-database datasource)]
    (p/add-migration-id db "12")
    (p/add-migration-id db "13")
    (p/add-migration-id db "20")
    (is (= ["12" "13" "20"] (p/applied-migration-ids db)))
    (p/remove-migration-id db "13")
    (is (= ["12" "20"] (p/applied-migration-ids db)))))

(deftest test-migrations-table
  (let [db (jdbc/sql-database datasource
                              {:migrations-table "migrations"})]
    (p/add-migration-id db "12")
    (is (= ["12"]
           (->> ["SELECT * FROM migrations"]
                (sql/query (:datasource db))
                (map :MIGRATIONS/ID)))))

  (n.j/execute! datasource ["CREATE SCHEMA foo"])
  (let [db (jdbc/sql-database datasource
                              {:migrations-table "foo.migrations"})]
    (p/add-migration-id db "20")
    (p/add-migration-id db "21")
    (is (= ["20" "21"]
           (->> ["SELECT * FROM foo.migrations"]
                (n.j/execute! (:datasource db))
                (map :MIGRATIONS/ID)))))

  ;; Unquoted names are automatically upper-cased by the DB while
  ;; quoted ones are case-sensitive and used exactly as given
  (n.j/execute! datasource ["CREATE SCHEMA BAR"])
  (let [db (jdbc/sql-database datasource
                              {:migrations-table "\"BAR\".\"MIGRATIONS\""})]
    (p/add-migration-id db "30")
    (p/add-migration-id db "31")
    (is (= ["30" "31"]
           (->> ["SELECT * FROM bar.migrations"]
                (n.j/execute! (:datasource db))
                (map :MIGRATIONS/ID))))))

(deftest test-migrations-table-exists-sql
  (let [migrations-table-exists-sql (str "SELECT * FROM INFORMATION_SCHEMA.TABLES"
                                         " WHERE TABLE_CATALOG='TESTDB'"
                                         " AND TABLE_NAME='RAGTIME_MIGRATIONS'")
        db (jdbc/sql-database datasource
                              {:migrations-table-exists-sql migrations-table-exists-sql})]
    (p/add-migration-id db "12")
    (is (= ["12"]
           (->> ["SELECT * FROM ragtime_migrations"]
                (sql/query (:datasource db))
                (map :RAGTIME_MIGRATIONS/ID))))

    (p/add-migration-id db "13")
    (is (= ["12" "13"]
           (->> ["SELECT * FROM ragtime_migrations"]
                (sql/query (:datasource db))
                (map :RAGTIME_MIGRATIONS/ID))))))

(defn table-names [db]
  (set (map :TABLES/TABLE_NAME (sql/query (:datasource db) ["SHOW TABLES"]))))

(defn test-sql-migration [connectable migration-extras]
  (let [db (jdbc/sql-database connectable)
        m  (jdbc/sql-migration
            (merge {:id   "01"
                    :up   ["CREATE TABLE foo (id int)"]
                    :down ["DROP TABLE foo"]}
                   migration-extras))]
    (core/migrate db m)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO"} (table-names db)))
    (core/rollback db m)
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))))

(deftest test-sql-migration-using-db-spec
  (test-sql-migration datasource {}))

(deftest test-sql-migration-without-transaction
  (test-sql-migration datasource {:transactions false}))

(deftest test-sql-migration-with-up-transaction
  (test-sql-migration datasource {:transactions :up}))

(deftest test-sql-migration-with-down-transaction
  (test-sql-migration datasource {:transactions :down}))

(deftest test-sql-migration-with-both-transaction
  (test-sql-migration datasource {:transactions :both})
  (test-sql-migration datasource {:transactions true}))

(deftest test-sql-migration-using-db-spec-with-existing-connection
  (with-open [conn (n.j/get-connection datasource)]
    (test-sql-migration conn {})))

(deftest test-load-directory
  (let [db  (jdbc/sql-database datasource)
        ms  (jdbc/load-directory "test/migrations")
        idx (core/into-index ms)]
    (core/migrate-all db idx ms)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO" "BAR" "BAZ"
             "QUZA" "QUZB" "QUXA" "QUXB" "LAST_TABLE" "QUXC" "QUXD"}
           (table-names db)))
    (is (= ["001-test" "002-bar" "003-test" "004-test" "005-test" "006-test" "007-test"]
           (p/applied-migration-ids db)))
    (core/rollback-last db idx (count ms))
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))
    (is (empty? (p/applied-migration-ids db)))))

(deftest test-load-resources
  (let [db  (jdbc/sql-database datasource)
        ms  (jdbc/load-resources "migrations")
        idx (core/into-index ms)]
    (core/migrate-all db idx ms)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO" "BAR" "BAZ"
             "QUZA" "QUZB" "QUXA" "QUXB" "LAST_TABLE" "QUXC" "QUXD"}
           (table-names db)))
    (is (= ["001-test" "002-bar" "003-test" "004-test" "005-test" "006-test" "007-test"]
           (p/applied-migration-ids db)))
    (core/rollback-last db idx (count ms))
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))
    (is (empty? (p/applied-migration-ids db)))))

(deftest test-migration-ordering
  (let [ids   (for [i (range 10000)] (format "%04d-test" i))
        files (mapcat (fn [id] [(str id ".up.sql") (str id ".down.sql")]) ids)]
    (with-redefs [file-seq (constantly (map io/file (shuffle files)))
                  slurp    (constantly "SELECT 1;")]
      (let [migrations (jdbc/load-directory "foo")]
        (is (= (count migrations) 10000))
        (is (= (map :id migrations) ids))))))
