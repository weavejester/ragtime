(ns ragtime.strategy-test
  (:require [clojure.test :refer [are deftest testing]]
            [ragtime.strategy :as strategy]))

(deftest apply-new-test
  (are [a m r] (= (strategy/apply-new a m) r)
    [:a] [:a] []
    [] [] []
    [:a :b] [:a :b] []
    [:a] [:a :b] [[:migrate :b]]
    [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
    [] [:a :b] [[:migrate :a] [:migrate :b]]
    [:a] [:b] [[:migrate :b]]
    [:a :b :c] [:a :d :c] [[:migrate :d]]))

(deftest raise-error-test
  (are [a m r] (= (strategy/raise-error a m) r)
    [:a] [:a] []
    [] [] []
    [:a :b] [:a :b] []
    [:a] [:a :b] [[:migrate :b]]
    [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
    [] [:a :b] [[:migrate :a] [:migrate :b]])
  (are [a m] (thrown? clojure.lang.ExceptionInfo (strategy/raise-error a m))
    [:a] [:b]
    [:a :b :c] [:a :d :c]
    [:a :b :c] [:a]))

(deftest rebase-test
  (are [a m r] (= (strategy/rebase a m) r)
    [:a] [:a] []
    [] [] []
    [:a :b] [:a :b] []
    [:a] [:a :b] [[:migrate :b]]
    [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
    [] [:a :b] [[:migrate :a] [:migrate :b]]
    [:a] [:b] [[:rollback :a] [:migrate :b]]
    [:a :b :c] [:a :d :c] [[:rollback :c] [:rollback :b]
                           [:migrate :d] [:migrate :c]]))

(deftest ignore-future-test
  (testing "Compatible migrations (same as raise-error)"
    (are [a m r] (= (strategy/ignore-future a m) r)
      [:a] [:a] []
      [] [] []
      [:a :b] [:a :b] []
      [:a] [:a :b] [[:migrate :b]]
      [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
      [] [:a :b] [[:migrate :a] [:migrate :b]]))
  (testing "Future migrations are ok"
    (are [a m r] (= (strategy/ignore-future a m) r)
      [:a :b :c] [:a] []))
  (testing "Incompatible migrations"
    (are [a m] (thrown? clojure.lang.ExceptionInfo (strategy/ignore-future a m))
      [:a] [:b]
      [:a :b :c] [:a :d :c]
      [:a :b :c] [:a :c :b])))
