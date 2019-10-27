(ns ragtime.strategy
  "Algorithms for managing conflicts between migrations applied to a database,
  and migrations that we want to apply to the database.")

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
       (drop-while (fn [[a m]] (= a m)))
       (unzip)
       (map (partial remove nil?))))

(defn apply-new
  "A strategy to apply any new migrations, irregardless of whether they come
  before migrations already applied to the database."
  [applied migrations]
  (let [unapplied (remove (set applied) migrations)]
    (for [m unapplied] [:migrate m])))

(defn- throw-conflict-exception [conflicts unapplied]
  (throw (ex-info (str "Conflict! Expected " (first unapplied)
                       " but " (first conflicts) " was applied.")
                  {:reason   ::migration-conflict
                   :expected (first unapplied)
                   :found    (first conflicts)})))

(defn raise-error
  "A strategy that raises an error if there are any conflicts between the
  applied migrations and the defined migration list. This is useful for
  production use."
  [applied migrations]
  (let [[conflicts unapplied] (split-at-conflict applied migrations)]
    (if (seq conflicts)
      (throw-conflict-exception conflicts unapplied)
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

(defn ignore-future
  "A strategy that raises an error if there are any conflicts between the
  applied migrations and the defined migration list, unless the conflicts are
  just future migrations.

  This is useful for blue/green deployments where the new
  version of the application has applied some migrations that the old version is not
  aware of. With this strategy, the old version expects that some migrations may exists
  that it is not aware of."
  [applied migrations]
  (let [[conflicts unapplied] (split-at-conflict applied migrations)]
    (if (and (seq conflicts)
             (seq unapplied))
      (throw-conflict-exception conflicts unapplied)
      (for [m unapplied] [:migrate m]))))
