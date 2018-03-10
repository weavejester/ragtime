(ns migrations.008-test
  (:require
   [clojure.java.jdbc :as jdbc]))

(defn up [db-spec]
  (jdbc/db-do-prepared db-spec
                       "CREATE TABLE cljt_2 (id int);")
  (jdbc/insert! db-spec "cljt_2" {:id 1}))

(defn down [db-spec]
  (jdbc/db-do-prepared db-spec "DROP TABLE cljt_2;"))
