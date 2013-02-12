(ns ragtime.test.main
  (:use [ragtime.main]
        [clojure.test]))

(deftest test-migration-up-filename
  (let [migration-file (first (migration-filenames "test"))]
    (is (re-find #"^migrations/[\d]{14}-test.up.sql$" migration-file))))

(deftest test-migration-down-filename
  (let [migration-file (second (migration-filenames "test"))]
    (is (re-find #"^migrations/[\d]{14}-test.down.sql$" migration-file))))

(deftest test-up-and-down-have-same-timestamp
  (let [[up-migration-file down-migration-file] (migration-filenames "test")
        up-migration-file-timestamp (re-find #"[\d]{14}" up-migration-file)
        down-migration-file-timestamp (re-find #"[\d]{14}" down-migration-file)]

    (is (not (nil? up-migration-file-timestamp)))
    (is (not (nil? down-migration-file-timestamp)))

    (is (= up-migration-file-timestamp down-migration-file-timestamp))))