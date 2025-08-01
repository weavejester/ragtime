(ns migrations.010-next-jdbc-test
  (:require [next.jdbc :as jdbc]))

(defn down [datasource]
  (jdbc/execute! datasource ["DROP TABLE clj_mixed2"]))
