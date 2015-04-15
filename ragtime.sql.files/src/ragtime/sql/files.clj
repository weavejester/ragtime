(ns ragtime.sql.files
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(ragtime.sql.database/require-jdbc 'sql)

(defn- debug? 
  (System/getenv "DEBUG"))

(def ^:private migration-pattern
  #"(.*)\.(up|down)\.sql$")

(defn- migration? [file]
  (re-find migration-pattern (.getName (io/file file))))

(defn- migration-id [file]
  (second (re-find migration-pattern (.getName (io/file file)))))

(defn- assert-migrations-complete [migration-files]
  (let [incomplete-files (remove #(= (count (val %)) 2)
                                 migration-files)]
    (assert (empty? incomplete-files)            
            (str "Incomplete migrations found. "
                 "Please provide up and down migration files for "
                 (str/join ", " (keys incomplete-files))))))

(defn- warn-on-non-migration-files [files]
  (let [unmatched (filter #(not (migration? %)) files)]
    (when (and (debug?) (not (empty? unmatched)))
      (do
        (println "Warning! Found files that doesn't match the migration pattern (" migration-pattern "): ")
        (doall (map println unmatched)))))
  files)

(defn- get-migration-files [dir]
  (let [files (->> (.listFiles (io/file dir))
                   (warn-on-non-migration-files)
                   (filter migration?)
                   (sort)
                   (group-by migration-id))]
    (assert-migrations-complete files)
    files))

;; Lexer borrowed from Clout

(defn- lex-1 [src clauses]
  (some
    (fn [[re action]]
      (let [matcher (re-matcher re src)]
        (if (.lookingAt matcher)
          [(if (fn? action) (action matcher) action)
           (subs src (.end matcher))])))
    (partition 2 clauses)))

(defn- lex [src & clauses]
  (loop [results []
         src     src
         clauses clauses]
    (if-let [[result src] (lex-1 src clauses)]
      (let [results (conj results result)]
        (if (= src "")
          results
          (recur results src clauses))))))

(defn- quoted-string [quote]
  (re-pattern
   (str quote "(?:[^" quote "]|\\\\" quote ")*" quote)))

(def ^:private sql-end-marker
  "__END_OF_SQL_SCRIPT__")

(defn- mark-sql-statement-ends [sql]
  (apply str
    (lex sql
      (quoted-string \') #(.group %)
      (quoted-string \") #(.group %)
      (quoted-string \`) #(.group %)
      #"[^'\"`;]+"       #(.group %)
      #";"               sql-end-marker)))

(defn- split-sql [sql]
  (-> (mark-sql-statement-ends sql)
      (str/split (re-pattern sql-end-marker))))

(defn sql-statements
  "Split a SQL script into its component statements."
  [sql]
  (->> (split-sql sql)
       (map str/trim)
       (remove str/blank?)))

(defn postgres? [conn]
  (-> conn class .getName (.contains "postgresql")))

(defn- print-next-ex-trace [e]
  (when e
    (when-let [next-e (.getNextException e)]
      (.printStackTrace next-e))))

(defn- run-sql-fn [file]
  (when (debug?) (println (str "running migration file: " file)))
  (fn [db]
    (sql/with-connection db
      (sql/transaction
       (try
         (if (postgres? (sql/connection))
           (sql/do-prepared (slurp file))
           (doseq [s (sql-statements (slurp file))]
             (sql/do-commands s)))
         (catch java.sql.BatchUpdateException e
           (print-next-ex-trace e)
           (throw e))
         (catch java.sql.SQLException e
           (print-next-ex-trace e)
           (throw e)))))))

(defn- make-migration [[id [down up]]]
  {:id   id
   :up   (run-sql-fn up)
   :down (run-sql-fn down)})

(def ^:private default-dir "migrations")

(defn migrations
  "Return a list of migrations to apply."
  ([] (migrations default-dir))
  ([dir]
     (->> (get-migration-files dir)
          (map make-migration)
          (sort-by :id))))
