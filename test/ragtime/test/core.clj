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

(deftest test-defmigration
  (clear-migrations! (ns-name *ns*))
  (testing "id based on symbol"
    (defmigration test-migrate-1)
    (is (= (id test-migrate-1)
           (str `test-migrate-1))))
  (testing "migrations listed in order"
    (defmigration test-migrate-2)
    (defmigration test-migrate-3)
    (is (= (list-migrations (ns-name *ns*))
           [test-migrate-1
            test-migrate-2
            test-migrate-3]))))
