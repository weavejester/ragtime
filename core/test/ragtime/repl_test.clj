(ns ragtime.repl-test
  (:require [clojure.test :refer :all]
            [ragtime.core-test :refer [in-memory-db assoc-migration]]
            [ragtime.repl :as repl]
            [ragtime.core :as core]))

(def migrations
  [(assoc-migration "a" :a 1)
   (assoc-migration "b" :b 2)
   (assoc-migration "c" :c 3)])

(deftest test-repl-functions
  (let [database (in-memory-db)
        config   {:datastore database :migrations migrations}]
    (is (= @(:data database) {:migrations #{}}))
    (is (= (with-out-str (repl/migrate config))
           (format "Applying a%nApplying b%nApplying c%n")))
    (is (= 1 (-> database :data deref :a)))
    (is (= 2 (-> database :data deref :b)))
    (is (= 3 (-> database :data deref :c)))
    (is (= (with-out-str (repl/rollback config))
           (format "Rolling back c%n")))
    (is (= 1 (-> database :data deref :a)))
    (is (= 2 (-> database :data deref :b)))
    (is (nil? (-> database :data deref :c)))
    (is (= (with-out-str (repl/rollback config 2))
           (format "Rolling back b%nRolling back a%n")))
    (is (nil? (-> database :data deref :a)))
    (is (nil? (-> database :data deref :b)))
    (is (nil? (-> database :data deref :c)))
    (is (= (with-out-str
             (repl/migrate config)
             (repl/rollback config "a"))
           (str (format "Applying a%nApplying b%nApplying c%n")
                (format "Rolling back c%nRolling back b%n"))))
    (is (= 1 (-> database :data deref :a)))
    (is (nil? (-> database :data deref :b)))
    (is (nil? (-> database :data deref :c)))))

(deftest test-custom-reporter
  (let [database (in-memory-db)
        config   {:datastore database
                  :migrations migrations
                  :reporter (fn [ds op id] (prn (type ds) op id))}]
    (is (= @(:data database) {:migrations #{}}))
    (is (= (with-out-str (repl/migrate config))
           (str (format "ragtime.core_test.InMemoryDB :up \"a\"%n")
                (format "ragtime.core_test.InMemoryDB :up \"b\"%n")
                (format "ragtime.core_test.InMemoryDB :up \"c\"%n"))))))

(deftest test-conflict-error
  (let [database            (in-memory-db)
        mixed-up-migrations [(migrations 0) (migrations 2) (migrations 1)]]
    (with-out-str (repl/migrate {:datastore database :migrations migrations}))
    (is (thrown-with-msg?
         Exception
         #"^Conflict! Expected c but b was applied\.$"
         (with-out-str
           (repl/migrate {:datastore database :migrations mixed-up-migrations}))))))
