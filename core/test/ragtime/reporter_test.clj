(ns ragtime.reporter-test
  (:require [clojure.test :refer [deftest is]]
            [ragtime.reporter :as reporter]))

(deftest silent-reporter-test
  (is (= (with-out-str (reporter/silent nil :up "foo")) "")))

(deftest print-reporter-test
  (is (= (with-out-str (reporter/print nil :up "foo"))
         (format "Applying foo%n")))
  (is (= (with-out-str (reporter/print nil :down "foo"))
         (format "Rolling back foo%n"))))
