(ns migrations.008-jdbc-test
  (:require [clojure.java.jdbc :as jdbc]))

(defn up [db-spec]
  (jdbc/execute! db-spec ["CREATE TABLE clj (id int)"]))

(defn down [db-spec]
  (jdbc/execute! db-spec ["DROP TABLE clj"]))
