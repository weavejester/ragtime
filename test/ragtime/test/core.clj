(ns ragtime.test.core
  (:use clojure.test
        ragtime.core
        ragtime.database)
  (:require [ragtime.strategy :as strategy]))

(defmigration test-migrate-1)

(defmigration test-migrate-2
  {:up   #(println "up")
   :down #(println "down")})

(deftest test-defmigration
  (testing "id based on symbol"
    (is (= (:id test-migrate-1)
           "ragtime.test.core/test-migrate-1")))
  (testing "migrations listed in order"
    (is (= (list-migrations 'ragtime.test.core)
           [test-migrate-1
            test-migrate-2])))
  (testing "retains up and down keys"
    (is (:up test-migrate-2))
    (is (:down test-migrate-2))))

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
        m  {:id "m"
            :up #(swap! a assoc :x 1)
            :down #(swap! a dissoc :x)}]
    (testing "migrate"
      (migrate db m)
      (is (= @a {:x 1}))
      (is (contains? (set (applied-migrations db)) m)))
    (testing "rollback"
      (rollback db m)
      (is (= @a {}))
      (is (not (contains? (set (applied-migrations db)) m))))))

(deftest test-migrate-all
  (let [store   (atom {})
        db      (in-memory-db)
        assoc-x {:id "assoc-x"
                 :up #(swap! store assoc :x 1)
                 :down #(swap! store dissoc :x)}
        assoc-y {:id "assoc-y"
                 :up #(swap! store assoc :y 2)
                 :down #(swap! store dissoc :y)}
        assoc-z {:id "assoc-z"
                 :up #(swap! store assoc :z 3)
                 :down #(swap! store dissoc :z)}]
    (migrate-all db [assoc-x assoc-y])
    (is (= @store {:x 1 :y 2}))
    (migrate-all db [assoc-x assoc-z] strategy/rebase)
    (is (= @store {:x 1 :z 3}))))
