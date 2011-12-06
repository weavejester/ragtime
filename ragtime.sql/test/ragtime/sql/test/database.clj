(ns ragtime.sql.test.database
  (:use [clojure.test]
        [ragtime.sql.database]
        [ragtime.core :only [add-migration-id remove-migration-id
                             applied-migration-ids]])
  (:require [clojure.java.jdbc :as sql]))

(def test-db (make-database {:classname "org.h2.Driver"
                             :subprotocol "h2"
                             :subname "mem:test_db"}))

(defn h2-fixture [f]
  (sql/with-connection test-db
    (f)))

(use-fixtures :each h2-fixture)

(deftest test-add-migrations
  (add-migration-id test-db "12")
  (add-migration-id test-db "13")
  (add-migration-id test-db "20")
  (is (= ["12" "13" "20"] (applied-migration-ids test-db)))
  (remove-migration-id test-db "13")
  (is (= ["12" "20"] (applied-migration-ids test-db))))

(deftest test-find-migrations
  (let [migrations-found (migrations "test-resources/migrations")]
    (is (every? fn? (map :up migrations-found)))
    (is (every? fn? (map :down migrations-found)))
    (is (= [{:id "20111202110600-create-foo-table", :order "20111202110600"}
            {:id "20111202113000-create-bar-table", :order "20111202113000"}]
           (for [m migrations-found]
             (dissoc m :up :down))))))

(deftest test-running-migrations
  (sql/with-connection test-db
    (-main "test-resources/migrations" test-db)
    (sql/insert-values :foo [:id] [1000] [2000])
    (sql/insert-values :bar [:id] [1000] [2000])
    (is (= [1000 2000] (sql/with-query-results results ["select * from foo"]
                         (vec (sort (map :id results))))))
    (is (= [1000 2000] (sql/with-query-results results ["select * from bar"]
                         (vec (sort (map :id results))))))))