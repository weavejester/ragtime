(ns ragtime.sql.compiler
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]))

(defmulti compile-expr
  (fn [_state [key & _args]] key))

(defmulti gen-id
  (fn [[key & _args]] key))

(defmethod gen-id :create-table [[_ table]]     (str "create-table-" table))
(defmethod gen-id :drop-table   [[_ table]]     (str "drop-table-" table))
(defmethod gen-id :add-column   [[_ table col]] (str "add-column-" table "-" col))
(defmethod gen-id :drop-column  [[_ table col]] (str "drop-column-" table "-" col))

(defn- normalize-migration [migration]
  (if (vector? migration)
    {:id (gen-id migration), :do migration}
    migration))

(defn- compile-migration [state migration]
  (if-some [expr (:do migration)]
    (let [{:keys [state up down]} (compile-expr state expr)
          migration (-> migration
                        (dissoc :do)
                        (assoc :up up, :down down))]
      (update state :migrations conj migration))
    (update state :migrations conj migration)))

(defn compile [migrations]
  (:migrations (transduce (map normalize-migration)
                          (completing compile-migration)
                          {:migrations []}
                          migrations)))

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
   :down  (str "CREATE TABLE " name " (" (column-sql (tables name)) ")")})

(defmethod compile-expr :add-column [state [_ table column definition]]
  {:state (assoc-in state [:tables table column] definition)
   :up    (str "ALTER TABLE " table " ADD COLUMN " (name column) " " definition)
   :down  (str "ALTER TABLE " table " DROP COLUMN " (name column))})

(defmethod compile-expr :drop-column [state [_ table column]]
  {:state (update-in state [:tables table] dissoc column)
   :up    (str "ALTER TABLE " table " DROP COLUMN " (name column))
   :down  (str "ALTER TABLE " table " ADD COLUMN " (name column) " "
               (get-in state [:tables table column]))})
