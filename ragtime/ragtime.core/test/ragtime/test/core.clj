(ns ragtime.test.core
  (:require [clojure.test :refer :all]
            [ragtime.core :refer :all]
            [ragtime.strategy :as strategy]))

(defrecord InMemoryDB [data]
  Migratable
  (add-migration-id [_ id]
    (swap! data update-in [:migrations] conj id))
  (remove-migration-id [_ id]
    (swap! data update-in [:migrations] disj id))
  (applied-migration-ids [_]
    (seq (:migrations @data))))

(defn in-memory-db []
  (InMemoryDB. (atom {:migrations #{}})))

(defn- assoc-migration [id key val]
  {:id id
   :up   (fn [db] (swap! (:data db) assoc key val))
   :down (fn [db] (swap! (:data db) dissoc key))})

(deftest test-migrate-and-rollback
  (let [database  (in-memory-db)
        migration (assoc-migration "m" :x 1)]
    (testing "migrate"
      (migrate database migration)
      (is (= (:x @(:data database)) 1))
      (is (contains? (set (applied-migrations database)) migration)))
    (testing "rollback"
      (rollback database migration)
      (is (nil? (:x @(:data database))))
      (is (not (contains? (set (applied-migrations database)) migration))))))

(deftest test-migrate-all
  (let [database (in-memory-db)
        assoc-x  (assoc-migration "assoc-x" :x 1)
        assoc-y  (assoc-migration "assoc-y" :y 2)
        assoc-z  (assoc-migration "assoc-z" :z 3)]
    (migrate-all database [assoc-x assoc-y])
    (is (= (:x @(:data database)) 1))
    (is (= (:y @(:data database)) 2))
    (migrate-all database [assoc-x assoc-z] strategy/rebase)
    (is (= (:x @(:data database)) 1))
    (is (nil? (:y @(:data database))))
    (is (= (:z @(:data database)) 3))))

(deftest test-migrate-all-apply-new
  "Tests migrate-all, with the apply-new strategy, in the scenario where a
  migration id is saved to a Migratable that persists after the application
  terminates. On the next call to migrate-all, migrations present in the
  Migratable should not be applied again, even if they have not yet been
  remembered by the application."
  (let [database (in-memory-db)
        assoc-y  (assoc-migration "assoc-y" :y 2)
        id       (:id assoc-y)]
    ; add the migration id to the database to simulate that it has been applied
    (add-migration-id database id)
    ; but remove it from the remembered migrations.
    (swap! defined-migrations dissoc id)
    (migrate-all database [assoc-y] strategy/apply-new)
    (is (nil? (:y @(:data database))))))

(deftest test-rollback-last
  (let [database (in-memory-db)
        assoc-x  (assoc-migration "assoc-x" :x 1)
        assoc-y  (assoc-migration "assoc-y" :y 2)
        assoc-z  (assoc-migration "assoc-z" :z 3)]
    (migrate-all database [assoc-x assoc-y assoc-z])
    (rollback-last database)
    (is (= (:x @(:data database)) 1))
    (is (= (:y @(:data database)) 2))
    (is (not (contains? @(:data database) :z)))
    (rollback-last database 2)
    (is (not (contains? @(:data database) :y)))
    (is (not (contains? @(:data database) :z)))))

(deftest test-rollback-to
  (let [database (in-memory-db)
        assoc-x  (assoc-migration "assoc-x" :x 1)
        assoc-y  (assoc-migration "assoc-y" :y 2)
        assoc-z  (assoc-migration "assoc-z" :z 3)]
    (migrate-all database [assoc-x assoc-y assoc-z])
    (rollback-to database "assoc-x")
    (is (= (:x @(:data database)) 1))
    (is (not (contains? @(:data database) :y)))
    (is (not (contains? @(:data database) :z)))))
