(ns ragtime.sql.test.files
  (:require [clojure.java.jdbc :as sql]
            [clojure.string :as str])
  (:use clojure.test
        ragtime.sql.files
        ragtime.sql.database
        ragtime.core))

(def test-db
  (connection "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1"))

(defn table-exists? [table-name]
  (not-empty
   (sql/with-query-results rs
     ["select true from information_schema.tables where table_name = ?"
      (str/upper-case table-name)]
     (vec rs))))

(deftest test-sql-statements
  (are [x y] (= (sql-statements x) y)
    "foo;bar"    ["foo" "bar"]
    "foo;; bar;" ["foo" "bar"]
    "'foo;bar'"  ["'foo;bar'"]
    "`foo;bar`"  ["`foo;bar`"]
    "\"fo;ba\""  ["\"fo;ba\""]
    "'a;b' c; d" ["'a;b' c" "d"]))

(deftest test-migrations
  (testing "no migration directory"
    (is (= (migrations) [])))
  (testing "custom migration directory"
    (let [migs (migrations "test/migrations")]
      (is (= (count migs) 1))
      (is (= (:id (first migs)) "20111202110600-create-foo-table"))
      (migrate-all test-db migs)
      (sql/with-connection test-db
        (is (table-exists? "ragtime_migrations"))
        (is (table-exists? "foo"))))))

(deftest test-migration-up-filename
  (let [migration-file (first (migration-filenames "test"))]
    (is (re-find #"^migrations/[\d]{14}-test.up.sql$" migration-file))))

(deftest test-migration-down-filename
  (let [migration-file (second (migration-filenames "test"))]
    (is (re-find #"^migrations/[\d]{14}-test.down.sql$" migration-file))))

(deftest test-up-and-down-have-same-timestamp
  (let [[up-migration-file down-migration-file] (migration-filenames "test")
        up-migration-file-timestamp (re-find #"[\d]{14}" up-migration-file)
        down-migration-file-timestamp (re-find #"[\d]{14}" down-migration-file)]

    (is (not (nil? up-migration-file-timestamp)))
    (is (not (nil? down-migration-file-timestamp)))

    (is (= up-migration-file-timestamp down-migration-file-timestamp))))