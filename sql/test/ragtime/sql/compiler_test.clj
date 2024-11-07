(ns ragtime.sql.compiler-test
  (:require [clojure.test :refer [deftest is]]
            [ragtime.sql.compiler :as compiler]))

(deftest test-compile
  (is (=  [{:id "create-table-foo"
            :up "CREATE TABLE foo (id int primary key, full_name text)"
            :down "DROP TABLE foo"}
           {:id "add-column-foo-updated_at"
            :up "ALTER TABLE foo ADD COLUMN updated_at timestamp default now()"
            :down "ALTER TABLE foo DROP COLUMN updated_at"}
           {:id "rename-column-foo-full_name-to-name"
            :up "ALTER TABLE foo RENAME COLUMN full_name TO name"
            :down "ALTER TABLE foo RENAME COLUMN name TO full_name"}
           {:id "create-index-foo_name"
            :up "CREATE INDEX foo_name ON TABLE foo (name)",
            :down "DROP INDEX foo_name"}
           {:id "drop-index-foo_name"
            :up "DROP INDEX foo_name"
            :down "CREATE INDEX foo_name ON TABLE foo (name)"}
           {:id "drop-column-foo-name"
            :up "ALTER TABLE foo DROP COLUMN name"
            :down "ALTER TABLE foo ADD COLUMN name text"}
           {:id "drop-table-foo"
            :up "DROP TABLE foo"
            :down (str "CREATE TABLE foo (id int primary key,"
                       " updated_at timestamp default now())")}]
          (compiler/compile
           '[[:create-table foo [id "int primary key"] [full_name "text"]]
             [:add-column foo updated_at "timestamp default now()"]
             [:rename-column foo full_name name]
             [:create-index foo_name foo [name]]
             [:drop-index foo_name]
             [:drop-column foo name]
             [:drop-table foo]]))))
