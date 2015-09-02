(ns ragtime.repl-test
  (:require [clojure.test :refer :all]
            [ragtime.core-test :refer [in-memory-db assoc-migration]]
            [ragtime.repl :as repl]
            [ragtime.core :as core]))

(def test-datetime #"^\d{14}$")
(def test-migration-path #"^resources/migrations/\d{14}_test.edn$")

(def migrations
  [(assoc-migration "a" :a 1)
   (assoc-migration "b" :b 2)
   (assoc-migration "c" :c 3)])

(defn- contains-string? [matcher r-string]
  (string? (re-find matcher r-string)))

(deftest test-repl-functions
  (let [database (in-memory-db)
        config   {:datastore database :migrations migrations}]
    (is (= @(:data database) {:migrations #{}}))
    (is (= (with-out-str (repl/migrate config))
           "Applying a\nApplying b\nApplying c\n"))
    (is (= 1 (-> database :data deref :a)))
    (is (= 2 (-> database :data deref :b)))
    (is (= 3 (-> database :data deref :c)))
    (is (= (with-out-str (repl/rollback config))
           "Rolling back c\n"))
    (is (= 1 (-> database :data deref :a)))
    (is (= 2 (-> database :data deref :b)))
    (is (nil? (-> database :data deref :c)))
    (is (= (with-out-str (repl/rollback config 2))
           "Rolling back b\nRolling back a\n"))
    (is (nil? (-> database :data deref :a)))
    (is (nil? (-> database :data deref :b)))
    (is (nil? (-> database :data deref :c)))
    (is (= (with-out-str
             (repl/migrate config)
             (repl/rollback config "a"))
           "Applying a\nApplying b\nApplying c\nRolling back c\nRolling back b\n"))
    (is (= 1 (-> database :data deref :a)))
    (is (nil? (-> database :data deref :b)))
    (is (nil? (-> database :data deref :c)))))

(deftest test-custom-reporter
  (let [database (in-memory-db)
        config   {:datastore database :migrations migrations :reporter prn}]
    (is (= @(:data database) {:migrations #{}}))
    (is (= (with-out-str (repl/migrate config))
           ":up \"a\"\n:up \"b\"\n:up \"c\"\n"))))

(deftest test-now
  (testing "Creates a DateTime string"
    (is (contains-string? test-datetime (repl/now)))))

(deftest test-migration-file-path
  (testing "Creates a migration file path string"
    (is (contains-string? test-migration-path (repl/migration-file-path "test")))))
