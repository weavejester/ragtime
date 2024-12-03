(ns ragtime.sql.compiler
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]))

(defmulti gen-id
  (fn [[key & _args]] key))

(defmethod gen-id :create-table  [[_ table]] (str "create-table-" table))
(defmethod gen-id :drop-table    [[_ table]] (str "drop-table-" table))
(defmethod gen-id :add-column    [[_ t col]] (str "add-column-" t "-" col))
(defmethod gen-id :drop-column   [[_ t col]] (str "drop-column-" t "-" col))
(defmethod gen-id :create-index  [[_ index]] (str "create-index-" index))
(defmethod gen-id :drop-index    [[_ index]] (str "drop-index-" index))
(defmethod gen-id :rename-column [[_ table old new]]
  (str "rename-column-" table "-" old "-to-" new))
(defmethod gen-id :create-unique-index [[_ index]]
  (str "create-unique-index-" index))

(defn- normalize [migration]
  (if (vector? migration)
    {:id (gen-id migration), :do [migration]}
    migration))

(defmulti ^:private compile-expr
  (fn [_state [key & _args]] key))

(defn- compile-do
  ([{:keys [migration] :as state}]
   (update state :migrations conj (dissoc migration :do)))
  ([state expr]
   (let [{:keys [state up down]} (compile-expr state expr)]
     (-> state
         (update-in [:migration :up] #(conj (or % []) up))
         (update-in [:migration :down] #(into [down] %))))))

(defn- compile-migration
  ([{:keys [migrations]}] migrations)
  ([state migration]
   (if-some [exprs (:do migration)]
     (transduce identity compile-do (assoc state :migration migration) exprs)
     (update state :migrations conj migration))))

(defn compile
  "Takes an ordered collection of migrations, and compiles the migrations using
  vector syntax into SQL. This replaces the :do key with the :up and :down keys
  on each affected migration. For example:

      [{:id \"x\" :do [[:create-table t [id \"int\"]]]}]

  Is converted into:

      [{:id \"x\" :up [\"CREATE TABLE t (id int)\"] :down [\"DROP TABLE t\"]}]

  Transactions may also be supplied as raw vectors, in which case an id will be
  auto generated:

      [[:create-table t [id \"int\"]]]

  This function is called automatically by ragtime.sql/load-migrations."
  [migrations]
  (transduce (map normalize) compile-migration {:migrations []} migrations))

(defn- column-map [columns]
  (reduce (fn [m [col def]] (assoc m col def)) {} columns))

(defn- column-sql [columns]
  (->> columns
       (map (fn [[col def]] (str (name col) " " def)))
       (str/join ", ")))

(defmethod compile-expr :create-table [state [_ name & columns]]
  {:state (update-in state [:tables name] merge (column-map columns))
   :up    (str "CREATE TABLE " name " (" (column-sql columns) ")")
   :down  (str "DROP TABLE " name)})

(defmethod compile-expr :drop-table [{:keys [tables] :as state} [_ name]]
  {:state (update state :tables dissoc name)
   :up    (str "DROP TABLE " name)
   :down  (str "CREATE TABLE " name " (" (column-sql (get tables name)) ")")})

(defmethod compile-expr :add-column [state [_ table column definition]]
  {:state (assoc-in state [:tables table column] definition)
   :up    (str "ALTER TABLE " table " ADD COLUMN " (name column) " " definition)
   :down  (str "ALTER TABLE " table " DROP COLUMN " (name column))})

(defmethod compile-expr :drop-column [state [_ table column]]
  {:state (update-in state [:tables table] dissoc column)
   :up    (str "ALTER TABLE " table " DROP COLUMN " (name column))
   :down  (str "ALTER TABLE " table " ADD COLUMN " (name column) " "
               (get-in state [:tables table column]))})

(defmethod compile-expr :rename-column [state [_ table old-col new-col]]
  {:state (-> state
              (update-in [:tables table] dissoc old-col)
              (update-in [:tables table] assoc new-col
                         (get-in state [:tables table old-col])))
   :up    (str "ALTER TABLE " table " RENAME COLUMN " (name old-col)
               " TO " (name new-col))
   :down  (str "ALTER TABLE " table " RENAME COLUMN " (name new-col)
               " TO " (name old-col))})

(defmethod compile-expr :create-index [state [_ name table columns]]
  {:state (assoc-in state [:indexes name] {:table table, :columns columns})
   :up    (str "CREATE INDEX " name " ON TABLE " table
               " (" (str/join ", " columns) ")")
   :down  (str "DROP INDEX " name)})

(defmethod compile-expr :drop-index [{:keys [indexes] :as state} [_ name]]
  {:state (update state :indexes dissoc name)
   :up    (str "DROP INDEX " name)
   :down  (str "CREATE INDEX " name " ON TABLE " (:table (get indexes name))
               " (" (str/join ", " (:columns (get indexes name))) ")")})

(defmethod compile-expr :create-unique-index [state [_ name table columns]]
  {:state (assoc-in state [:indexes name] {:table table, :columns columns})
   :up    (str "CREATE UNIQUE INDEX " name " ON TABLE " table
               " (" (str/join ", " columns) ")")
   :down  (str "DROP INDEX " name)})
