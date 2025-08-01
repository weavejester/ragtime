(ns migrations.009-jdbc-test
  (:require [clojure.java.jdbc :as jdbc]))

(defn up [db-spec]
  (jdbc/execute! db-spec ["CREATE TABLE clj_mixed1 (id int)"]))
