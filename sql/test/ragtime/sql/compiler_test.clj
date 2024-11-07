(ns ragtime.sql.compiler-test
  (:require [clojure.test :refer [deftest is]]
            [ragtime.sql.compiler :as compiler]))

(deftest test-compile
  (is (=  [{:id "create-table-foo"
            :up "CREATE TABLE foo (id int primary key, name text)"
            :down "DROP TABLE foo"}
           {:id "add-column-foo-updated_at"
            :up "ALTER TABLE foo ADD COLUMN updated_at timestamp default now()"
            :down "ALTER TABLE foo DROP COLUMN updated_at"}
           {:id "drop-column-foo-name"
            :up "ALTER TABLE foo DROP COLUMN name"
            :down "ALTER TABLE foo ADD COLUMN name text"}
           {:id "drop-table-foo"
            :up "DROP TABLE foo"
            :down (str "CREATE TABLE foo (id int primary key,"
                       " updated_at timestamp default now())")}]
          (compiler/compile
           '[[:create-table foo
              [id "int primary key"]
              [name "text"]]
             [:add-column foo updated_at "timestamp default now()"]
             [:drop-column foo name]
             [:drop-table foo]]))))
