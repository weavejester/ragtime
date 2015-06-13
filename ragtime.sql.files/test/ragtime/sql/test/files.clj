(ns ragtime.sql.test.files
  (:require [clojure.string :as str])
  (:use clojure.test
        ragtime.sql.files
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

(deftest test-sql-statements
  (are [x y] (= (sql-statements x) y)
    "foo;bar"          ["foo" "bar"]
    "foo;; bar;"       ["foo" "bar"]
    "'foo;bar'"        ["'foo;bar'"]
    "`foo;bar`"        ["`foo;bar`"]
    "\"fo;ba\""        ["\"fo;ba\""]
    "'a;b' c; d"       ["'a;b' c" "d"]
    "-"                ["-"]
    "--"               []
    "--\na"            ["a"]
    "a-b"              ["a-b"]
    "a;-b;c-d;e-"      ["a" "-b" "c-d" "e-"]
    "a'-'-'-';b"       ["a'-'-'-'" "b"]
    "a;\nb\n"          ["a" "b"]
    "a;\n--b;c"        ["a"]
    "a;\n--b\nc"       ["a" "c"]
    "a'--';b"          ["a'--'" "b"]
    "a'b--c'--\"\n;d"  ["a'b--c'" "d"]
    "a;\nb--'c\nd; e;" ["a" "b\nd" "e"]))

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

(deftest test-incomplete-migrations
  (testing "incomplete migrations"
    (is (thrown-with-msg? AssertionError #"Incomplete migrations"
                          (migrations "test/incomplete-migrations")))))
