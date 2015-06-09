(ns ragtime.sql.test.database
  (:require [clojure.test :refer :all]
            [ragtime.sql.database :refer :all]
            [ragtime.core :as ragtime]))

(def test-db
  (ragtime/connection "jdbc:h2:mem:test_db"))

(deftest test-add-migrations
  (ragtime/add-migration-id test-db "12")
  (ragtime/add-migration-id test-db "13")
  (ragtime/add-migration-id test-db "20")
  (is (= ["12" "13" "20"] (ragtime/applied-migration-ids test-db)))
  (ragtime/remove-migration-id test-db "13")
  (is (= ["12" "20"] (ragtime/applied-migration-ids test-db))))
