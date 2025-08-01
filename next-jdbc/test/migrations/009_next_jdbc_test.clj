(ns migrations.009-next-jdbc-test
  (:require [next.jdbc :as jdbc]))

(defn up [datasource]
  (jdbc/execute! datasource ["CREATE TABLE clj_mixed1 (id int)"]))
