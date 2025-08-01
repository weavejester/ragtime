(ns migrations.010-jdbc-test
  (:require [clojure.java.jdbc :as jdbc]))

(defn down [db-spec]
  (jdbc/execute! db-spec ["DROP TABLE clj_mixed2"]))
