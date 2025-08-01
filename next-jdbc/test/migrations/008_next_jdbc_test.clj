(ns migrations.008-next-jdbc-test
  (:require [next.jdbc :as jdbc]))

(defn up [datasource]
  (jdbc/execute! datasource ["CREATE TABLE clj (id int)"]))

(defn down [datasource]
  (jdbc/execute! datasource ["DROP TABLE clj"]))
