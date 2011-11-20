(ns ragtime.test.core
  (:use [ragtime.core]
        [clojure.test]))

(deftest test-migration
  (testing "migration id"
    (is (= (id (migration "foo"))
           "foo")))
  (let [s (atom {})
        m (migration "foo"
            (:up (swap! s assoc :x 1))
            (:down (swap! s dissoc :x)))]
    (testing "migration up"      
      (up m)
      (is (= @s {:x 1})))
    (testing "migration down"
      (down m)
      (is (= @s {})))))

(defmigration test-migrate-1)
(defmigration test-migrate-2)

(deftest test-defmigration
  (testing "id based on symbol"
    (is (= (id test-migrate-1)
           "ragtime.test.core/test-migrate-1")))
  (testing "migrations listed in order"
    (is (= (list-migrations 'ragtime.test.core)
           [test-migrate-1
            test-migrate-2]))))
