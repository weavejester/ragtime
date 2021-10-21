(ns ragtime.jdbc-test
  (:require [clojure.test :refer :all]
            [ragtime.jdbc :as jdbc]
            [ragtime.core :as core]
            [ragtime.protocols :as p]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def db-spec "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")

(use-fixtures :each (fn reset-db [f]
                      (sql/execute! db-spec "DROP ALL OBJECTS")
                      (f)))

(deftest test-add-migrations
  (let [db (jdbc/sql-database db-spec)]
    (p/add-migration-id db "12")
    (p/add-migration-id db "13")
    (p/add-migration-id db "20")
    (is (= ["12" "13" "20"] (p/applied-migration-ids db)))
    (p/remove-migration-id db "13")
    (is (= ["12" "20"] (p/applied-migration-ids db)))))

(deftest test-migrations-table
  (let [db (jdbc/sql-database db-spec
                              {:migrations-table "migrations"})]
    (p/add-migration-id db "12")
    (is (= ["12"]
           (sql/query (:db-spec db) ["SELECT * FROM migrations"] {:row-fn :id}))))

  (sql/execute! db-spec "CREATE SCHEMA myschema")
  (let [db (jdbc/sql-database db-spec
                              {:migrations-table "myschema.migrations"})]
    (p/add-migration-id db "20")
    (p/add-migration-id db "21")
    (is (= ["20" "21"]
           (sql/query (:db-spec db) ["SELECT * FROM myschema.migrations"] {:row-fn :id})))))

(defn table-names [db]
  (set (sql/query (:db-spec db) ["SHOW TABLES"] {:row-fn :table_name})))

(defn test-sql-migration [db-spec migration-extras]
  (let [db (jdbc/sql-database db-spec)
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
  (test-sql-migration db-spec {}))

(deftest test-sql-migration-without-transaction
  (test-sql-migration db-spec {:transactions false}))

(deftest test-sql-migration-with-up-transaction
  (test-sql-migration db-spec {:transactions :up}))

(deftest test-sql-migration-with-down-transaction
  (test-sql-migration db-spec {:transactions :down}))

(deftest test-sql-migration-with-both-transaction
  (test-sql-migration db-spec {:transactions :both})
  (test-sql-migration db-spec {:transactions true}))

(deftest test-sql-migration-using-db-spec-with-existing-connection
  (sql/with-db-connection
    [conn db-spec]
    (test-sql-migration conn {})))

(deftest test-load-directory
  (let [db  (jdbc/sql-database db-spec)
        ms  (jdbc/load-directory "test/migrations")
        idx (core/into-index ms)]
    (core/migrate-all db idx ms)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO" "BAR" "BAZ" "QUZA" "QUZB" "QUXA" "QUXB" "LAST_TABLE"}
           (table-names db)))
    (is (= ["001-test" "002-bar" "003-test" "004-test" "005-test" "006-test"]
           (p/applied-migration-ids db)))
    (core/rollback-last db idx (count ms))
    (is (= #{"RAGTIME_MIGRATIONS"} (table-names db)))
    (is (empty? (p/applied-migration-ids db)))))

(deftest test-load-resources
  (let [db  (jdbc/sql-database db-spec)
        ms  (jdbc/load-resources "migrations")
        idx (core/into-index ms)]
    (core/migrate-all db idx ms)
    (is (= #{"RAGTIME_MIGRATIONS" "FOO" "BAR" "BAZ" "QUZA" "QUZB" "QUXA" "QUXB" "LAST_TABLE"}
           (table-names db)))
    (is (= ["001-test" "002-bar" "003-test" "004-test" "005-test" "006-test"]
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
