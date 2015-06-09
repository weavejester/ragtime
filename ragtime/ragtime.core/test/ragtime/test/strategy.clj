(ns ragtime.test.strategy
  (:use clojure.test
        ragtime.strategy))

(deftest apply-new-test
  (are [a m r] (= (apply-new a m) r)
    [:a] [:a] []
    [] [] []
    [:a :b] [:a :b] []
    [:a] [:a :b] [[:migrate :b]]
    [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
    [] [:a :b] [[:migrate :a] [:migrate :b]]
    [:a] [:b] [[:migrate :b]]
    [:a :b :c] [:a :d :c] [[:migrate :d]]))

(deftest raise-error-test
  (are [a m r] (= (raise-error a m) r)
    [:a] [:a] []
    [] [] []
    [:a :b] [:a :b] []
    [:a] [:a :b] [[:migrate :b]]
    [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
    [] [:a :b] [[:migrate :a] [:migrate :b]])
  (are [a m] (thrown? Exception (raise-error a m))
    [:a] [:b]
    [:a :b :c] [:a :d :c]))

(deftest rebase-test
  (are [a m r] (= (rebase a m) r)
    [:a] [:a] []
    [] [] []   
    [:a :b] [:a :b] []
    [:a] [:a :b] [[:migrate :b]]
    [:a] [:a :b :c] [[:migrate :b] [:migrate :c]]
    [] [:a :b] [[:migrate :a] [:migrate :b]]
    [:a] [:b] [[:rollback :a] [:migrate :b]]
    [:a :b :c] [:a :d :c] [[:rollback :c] [:rollback :b]
                           [:migrate :d] [:migrate :c]]))
