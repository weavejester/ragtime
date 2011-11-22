(ns ragtime.test.core
  (:use clojure.test
        ragtime.core
        ragtime.database))

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

(deftype InMemoryDB [migrations]
  Database
  (add-migration-id [_ id]
    (swap! migrations conj id))
  (remove-migration-id [_ id]
    (swap! migrations disj id))
  (applied-migration-ids [_]
    (seq @migrations)))

(defn in-memory-db []
  (InMemoryDB. (atom #{})))

(deftest test-migrate-and-rollback
  (let [a  (atom {})
        db (in-memory-db)
        m  (migration "m"
             (:up (swap! a assoc :x 1))
             (:down (swap! a dissoc :x)))]
    (testing "migrate"
      (migrate db m)
      (is (= @a {:x 1}))
      (is (contains? (set (applied-migrations db)) m)))
    (testing "rollback"
      (rollback db m)
      (is (= @a {}))
      (is (not (contains? (set (applied-migrations db)) m))))))
