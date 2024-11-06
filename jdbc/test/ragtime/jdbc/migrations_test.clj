(ns ragtime.jdbc.migrations-test
  (:require [clojure.test :refer [deftest is]]
            [ragtime.jdbc :as jdbc]
            [ragtime.jdbc.migrations :as mig]))

(deftest create-table-test
  (is (= (mig/create-table ::foo :foo [[:id "int"] [:name "varchar(255)"]])
         (jdbc/sql-migration
          {:id   ::foo
           :up   ["CREATE TABLE foo (id int, name varchar(255))"]
           :down ["DROP TABLE foo"]}))))
