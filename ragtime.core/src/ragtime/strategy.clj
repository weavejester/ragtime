(ns ragtime.strategy
  "Algorithms for managing conflicts between migrations applied to a database,
  and migrations that we want to apply to the database."
  (:require [ragtime.protocols :as p]))

(defn- pad [colls]
  (let [n (apply max (map count colls))]
    (for [c colls]
      (concat c (repeat (- n (count c)) nil)))))

(defn- zip [& colls]
  (apply map vector (pad colls)))

(defn- unzip [[x & coll]]
  (if coll
    (lazy-seq (map cons x (unzip coll)))
    (map list x)))

(defn- split-at-conflict [applied migrations]
  (->> (zip applied migrations)
       (drop-while (fn [[a m]] (= (p/id a) (p/id m))))
       (unzip)
       (map (partial remove nil?))))

(defn apply-new
  "A strategy to apply any new migrations, irregardless of whether they come
  before migrations already applied to the database."
  [applied migrations]
  (let [unapplied (remove (set applied) migrations)]
    (for [m unapplied] [:migrate m])))

(defn raise-error
  "A strategy that raises an error if there are any conflicts between the
  applied migrations and the defined migration list. This is useful for
  production use."
  [applied migrations]
  (let [[conflicts unapplied] (split-at-conflict applied migrations)]
    (if (seq conflicts)
      (throw (Exception.
              (str "Conflict! Expected " (p/id (first unapplied))
                   " but " (p/id (first conflicts)) " was applied.")))
      (for [m unapplied] [:migrate m]))))

(defn rebase
  "A strategy that rollbacks the database to the first conflict, then applies
  the successive migrations in order. This is useful when developing, but is
  not suitable for production use."
  [applied migrations]
  (let [[conflicts unapplied] (split-at-conflict applied migrations)]
    (concat
     (for [c (reverse conflicts)] [:rollback c])
     (for [m unapplied] [:migrate m]))))
