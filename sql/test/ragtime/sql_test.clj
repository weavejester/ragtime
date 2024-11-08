(ns ragtime.sql-test
  (:require [clojure.test :refer [deftest is]]
            [ragtime.sql :as sql]))

(deftest test-load-directory
  (is (= [{:id "001-test", :transactions :both
           :up ["CREATE TABLE foo (id int)"], :down ["DROP TABLE foo"]}
          {:id "002-bar", :transactions :both
           :up ["CREATE TABLE bar (id int)"], :down ["DROP TABLE bar"]}
          {:id "003-test"
           :up ["CREATE TABLE baz (id int);\n"], :down ["DROP TABLE baz;\n"]}
          {:id "004-test"
           :up ["CREATE TABLE quza (id int);\n" "CREATE TABLE quzb (id int);\n"]
           :down ["DROP TABLE quzb;\n" "DROP TABLE quza;\n"]}
          {:id "005-test"
           :up ["CREATE TABLE quxa (id int);" "CREATE TABLE quxb (id int);\n"]
           :down ["DROP TABLE quxa;" "DROP TABLE quxb;\n"]}
          {:id "006-test", :transactions :both
           :up ["CREATE TABLE last_table (id int)"]
           :down ["DROP TABLE last_table"]}
          {:id "007-test"
           :up ["--\n-- ISSUE 159 Test\n--\nCREATE TABLE quxc (id int);"
                "CREATE TABLE quxd (id int);\n"]
           :down ["--\n-- ISSUE 159 Test\n--\nDROP TABLE quxc;"
                  "DROP TABLE quxd;\n"]}
          {:id "008-test", :transactions :both
           :up ["CREATE TABLE aaa (id int)"] :down ["DROP TABLE aaa"]}
          {:id "009-test", :transactions :both
           :up ["CREATE TABLE bbb (id int)"] :down ["DROP TABLE bbb"]}
          {:id "create-table-ccc", :transactions :both
           :up ["CREATE TABLE ccc (id int)"] :down ["DROP TABLE ccc"]}]
         (sql/load-directory "test/migrations"))))

(deftest test-load-resources
  (is (= [{:id "001-test", :transactions :both
           :up ["CREATE TABLE foo (id int)"], :down ["DROP TABLE foo"]}
          {:id "002-bar", :transactions :both
           :up ["CREATE TABLE bar (id int)"], :down ["DROP TABLE bar"]}
          {:id "003-test"
           :up ["CREATE TABLE baz (id int);\n"], :down ["DROP TABLE baz;\n"]}
          {:id "004-test"
           :up ["CREATE TABLE quza (id int);\n" "CREATE TABLE quzb (id int);\n"]
           :down ["DROP TABLE quzb;\n" "DROP TABLE quza;\n"]}
          {:id "005-test"
           :up ["CREATE TABLE quxa (id int);" "CREATE TABLE quxb (id int);\n"]
           :down ["DROP TABLE quxa;" "DROP TABLE quxb;\n"]}
          {:id "006-test", :transactions :both
           :up ["CREATE TABLE last_table (id int)"]
           :down ["DROP TABLE last_table"]}
          {:id "007-test"
           :up ["--\n-- ISSUE 159 Test\n--\nCREATE TABLE quxc (id int);"
                "CREATE TABLE quxd (id int);\n"]
           :down ["--\n-- ISSUE 159 Test\n--\nDROP TABLE quxc;"
                  "DROP TABLE quxd;\n"]}
          {:id "008-test", :transactions :both
           :up ["CREATE TABLE aaa (id int)"] :down ["DROP TABLE aaa"]}
          {:id "009-test", :transactions :both
           :up ["CREATE TABLE bbb (id int)"] :down ["DROP TABLE bbb"]}
          {:id "create-table-ccc", :transactions :both
           :up ["CREATE TABLE ccc (id int)"] :down ["DROP TABLE ccc"]}]
         (sql/load-resources "migrations"))))

(deftest test-load-migrations
  (is (= [{:id "008-test", :transactions :both
           :up ["CREATE TABLE aaa (id int)"] :down ["DROP TABLE aaa"]}
          {:id "009-test", :transactions :both
           :up ["CREATE TABLE bbb (id int)"] :down ["DROP TABLE bbb"]}
          {:id "create-table-ccc", :transactions :both
           :up ["CREATE TABLE ccc (id int)"] :down ["DROP TABLE ccc"]}]
         (sql/load-migrations "test/migrations/008-test.edn"))))
