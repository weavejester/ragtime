(ns ragtime.core-test
  (:require [clojure.test :refer :all]
            [ragtime.core :refer :all]
            [ragtime.protocols :as p]
            [ragtime.strategy :as strategy]
            [ragtime.reporter :as reporter]))

(defrecord InMemoryDB [data]
  p/DataStore
  (add-migration-id [_ id]
    (swap! data update-in [:migrations] conj id))
  (remove-migration-id [_ id]
    (swap! data update-in [:migrations] (partial filterv #(not= % id))))
  (applied-migration-ids [_]
    (seq (:migrations @data))))

(defn in-memory-db []
  (InMemoryDB. (atom {:migrations []})))

(defn assoc-migration [id key val]
  (reify p/Migration
    (id [_] id)
    (run-up! [_ store] (swap! (:data store) assoc key val))
    (run-down! [_ store] (swap! (:data store) dissoc key))))

(defn interrupted-migration []
  (reify p/Migration
    (id [_] "interrupted-migration")
    (run-up! [_ store]
      (.interrupt (Thread/currentThread))
      (swap! (:data store) assoc :interrupted? true))
    (run-down! [_ store]
      (swap! (:data store) dissoc :interrupted?))))

(deftest test-into-index
  (let [assoc-x (assoc-migration "assoc-x" :x 1)
        assoc-y (assoc-migration "assoc-y" :y 2)
        index   (into-index [assoc-x])]
    (is (= (index "assoc-x") assoc-x))
    (is (= ((into-index index [assoc-y]) "assoc-y") assoc-y))))

(deftest test-migrate-and-rollback
  (let [database  (in-memory-db)
        migration (assoc-migration "m" :x 1)
        index     (into-index [migration])]
    (testing "migrate"
      (migrate database migration)
      (is (= (:x @(:data database)) 1))
      (is (contains? (set (applied-migrations database index)) migration)))
    (testing "rollback"
      (rollback database migration)
      (is (nil? (:x @(:data database))))
      (is (not (contains? (set (applied-migrations database index)) migration))))))

(deftest test-migrate-all
  (let [database (in-memory-db)
        assoc-x  (assoc-migration "assoc-x" :x 1)
        assoc-y  (assoc-migration "assoc-y" :y 2)
        assoc-z  (assoc-migration "assoc-z" :z 3)
        index    (into-index [assoc-x assoc-y assoc-z])]
    (migrate-all database index [assoc-x assoc-y])
    (is (= (:x @(:data database)) 1))
    (is (= (:y @(:data database)) 2))
    (migrate-all database index [assoc-x assoc-z] {:strategy strategy/rebase})
    (is (= (:x @(:data database)) 1))
    (is (nil? (:y @(:data database))))
    (is (= (:z @(:data database)) 3))))

(deftest test-migrate-all-interrupted
  (let [database   (in-memory-db)
        assoc-x    (assoc-migration "assoc-x" :x 1)
        assoc-y    (assoc-migration "assoc-y" :y 2)
        migrations [assoc-x (interrupted-migration) assoc-y]
        exception  (volatile! nil)]
    (doto (Thread.
           #(try
              (migrate-all database {} migrations {:strategy strategy/rebase})
              (catch InterruptedException e
                (vreset! exception e))))
      (.start)
      (.join))
    (is (= ["assoc-x" "interrupted-migration"] (:migrations @(:data database))))
    (is (= 1 (:x @(:data database)))
        "Migration before interrupt is processed fully")
    (is (= true (:interrupted? @(:data database)))
        "Migration before interrupt is processed fully")
    (is (nil? (:y @(:data database))) "Migration after interrupt is ignored")
    (is (thrown? InterruptedException
                 #"Stopping running migrations before assoc-y"
                 (some-> @exception throw)))))

(deftest test-migrate-all-apply-new
  "Tests migrate-all, with the apply-new strategy, in the scenario where a
  migration id is saved to a Migratable that persists after the application
  terminates. On the next call to migrate-all, migrations present in the
  Migratable should not be applied again, even if they have not yet been
  remembered by the application."
  (let [database (in-memory-db)
        assoc-y  (assoc-migration "assoc-y" :y 2)]
    (p/add-migration-id database (p/id assoc-y))
    (migrate-all database {} [assoc-y] {:strategy strategy/apply-new})
    (is (nil? (:y @(:data database))))))

(deftest test-rollback-last
  (let [database   (in-memory-db)
        assoc-x    (assoc-migration "assoc-x" :x 1)
        assoc-y    (assoc-migration "assoc-y" :y 2)
        assoc-z    (assoc-migration "assoc-z" :z 3)
        migrations [assoc-x assoc-y assoc-z]]
    (migrate-all database {} migrations)
    (rollback-last database (into-index migrations))
    (is (= (:x @(:data database)) 1))
    (is (= (:y @(:data database)) 2))
    (is (not (contains? @(:data database) :z)))
    (rollback-last database (into-index migrations) 2)
    (is (not (contains? @(:data database) :y)))
    (is (not (contains? @(:data database) :z)))))

(deftest test-rollback-to
  (let [database   (in-memory-db)
        assoc-x    (assoc-migration "assoc-x" :x 1)
        assoc-y    (assoc-migration "assoc-y" :y 2)
        assoc-z    (assoc-migration "assoc-z" :z 3)
        migrations [assoc-x assoc-y assoc-z]]
    (migrate-all database {} migrations)
    (rollback-to database (into-index migrations) "assoc-x")
    (is (= (:x @(:data database)) 1))
    (is (not (contains? @(:data database) :y)))
    (is (not (contains? @(:data database) :z)))))

(deftest test-rollback-to-throws-exception-if-id-not-found
  (let [database   (in-memory-db)
        assoc-x    (assoc-migration "assoc-x" :x 1)
        assoc-y    (assoc-migration "assoc-y" :y 2)
        assoc-z    (assoc-migration "assoc-z" :z 3)
        migrations [assoc-x assoc-y assoc-z]]
    (migrate-all database {} migrations)
    (is (thrown? clojure.lang.ExceptionInfo
                 (rollback-to database (into-index migrations) "assoc-not-exists")))
    (is (= (:x @(:data database)) 1))
    (is (= (:y @(:data database)) 2))
    (is (= (:z @(:data database)) 3))
    (is (contains? @(:data database) :x))
    (is (contains? @(:data database) :y))
    (is (contains? @(:data database) :z))))

(deftest test-reporting
  (let [database   (in-memory-db)
        migrations [(assoc-migration "assoc-x" :x 1)
                    (assoc-migration "assoc-y" :y 2)]]
    (is (= (with-out-str
             (migrate-all database {} migrations
                          {:strategy strategy/rebase, :reporter reporter/print}))
           (format "Applying assoc-x%nApplying assoc-y%n")))
    (is (= (with-out-str
             (rollback-to database (into-index migrations) "assoc-x"
                          {:reporter reporter/print}))
           (format "Rolling back assoc-y%n")))
    (is (= (with-out-str
             (rollback-last database (into-index migrations) 1
                            {:reporter reporter/print}))
           (format "Rolling back assoc-x%n")))))
