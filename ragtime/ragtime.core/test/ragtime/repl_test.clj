(ns ragtime.repl-test
  (:require [clojure.test :refer :all]
            [ragtime.core-test :refer [in-memory-db assoc-migration]]
            [ragtime.repl :as repl]
            [ragtime.core :as core]))

(defn loader []
  [(assoc-migration "a" :a 1)
   (assoc-migration "b" :b 2)
   (assoc-migration "c" :c 3)])

(def database (in-memory-db))

(repl/set-config! {:loader loader, :database database})

(deftest test-repl-functions
  (is (= @(:data database) {:migrations #{}}))
  (repl/migrate)
  (is (= 1 (-> database :data deref :a)))
  (is (= 2 (-> database :data deref :b)))
  (is (= 3 (-> database :data deref :c)))
  (repl/rollback)
  (is (= 1 (-> database :data deref :a)))
  (is (= 2 (-> database :data deref :b)))
  (is (nil? (-> database :data deref :c)))
  (repl/rollback 2)
  (is (nil? (-> database :data deref :a)))
  (is (nil? (-> database :data deref :b)))
  (is (nil? (-> database :data deref :c))))
