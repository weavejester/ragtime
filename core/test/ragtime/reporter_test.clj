(ns ragtime.reporter-test
  (:require [clojure.test :refer :all]
            [ragtime.reporter :as reporter]))

(deftest silent-reporter-test
  (is (= (with-out-str (reporter/silent nil :up "foo")) "")))

(deftest print-reporter-test
  (is (= (with-out-str (reporter/print nil :up "foo"))   "Applying foo\n"))
  (is (= (with-out-str (reporter/print nil :down "foo")) "Rolling back foo\n")))
