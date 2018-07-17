(ns migrations.007-test
  (:require
    [clojure.java.jdbc :as jdbc]))

(defn up [db-spec]
  (jdbc/db-do-prepared db-spec "CREATE TABLE cljt_1 (id int);"))

(defn down [db-spec]
  (jdbc/db-do-prepared db-spec "DROP TABLE cljt_1;"))
