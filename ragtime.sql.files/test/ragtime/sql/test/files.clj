(ns ragtime.sql.test.files
  (:use clojure.test
        ragtime.sql.files))

(deftest test-migrations
  (is (= (migrations) [])))