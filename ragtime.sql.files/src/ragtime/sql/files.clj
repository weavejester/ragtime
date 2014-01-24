(ns ragtime.sql.files
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [clojure.java.classpath :as cp]
            [clojure.string :as str]))

(def ^:private migration-pattern
  #"(.*)\.(up|down)\.sql$")

(defn- migration? [url]
  (re-find migration-pattern (.getPath url)))

(defn- migration-id [url]
  (-> (second (re-find migration-pattern (.getPath url)))
      (str/replace #".*/" "")))

(defn- list-classpath-directories [path]
  (->> (cp/classpath-directories)
       (map #(io/file % path))
       (filter #(and (.exists %) (.isDirectory %)))
       (mapcat #(.listFiles %))
       (map io/as-url)))

(defn- list-classpath-jarfiles [path]
  (->> (mapcat cp/filenames-in-jar (cp/classpath-jarfiles))
       (filter #(.startsWith % path))
       (map #(io/resource %))))

(defmulti list-files (partial re-find #"\w*:"))

(defmethod list-files "classpath:" [path]
  (let [p (str/replace path "classpath:" "")]
    (concat (list-classpath-directories p)
            (list-classpath-jarfiles p))))

(defmethod list-files nil [path]
  (->> (io/file path)
       (.listFiles)
       (map io/as-url)))

(defn- get-migration-files [path]
  (->> (list-files path)
       (filter migration?)
       (sort-by #(.getPath %))
       (group-by migration-id)))

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

(defn- run-sql-fn [file]
  (fn [db]
    (sql/with-connection db
      (sql/transaction
       (doseq [s (sql-statements (slurp file))]
         (sql/do-commands s))))))

(defn- make-migration [[id [down up]]]
  {:id   id
   :up   (run-sql-fn up)
   :down (run-sql-fn down)})

(def ^:private default-path "migrations")

(defn migrations
  "Return a list of migrations to apply."
  ([] (migrations default-path))
  ([path]
     (->> (get-migration-files path)
          (map make-migration)
          (sort-by :id))))
