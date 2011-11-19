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
