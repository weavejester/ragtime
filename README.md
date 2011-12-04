# Ragtime

Ragtime is a Clojure library for migrating structured data, and
consists of ragtime.core, which is database-independent, and
ragtime.sql, which includes functions suitable for migration SQL
database schema.

## Installation

Add ragtime.core as a dependency if you want the database-independent
core:

    [ragtime/ragtime.core "0.1.1"]

The ragtime.sql library is currently not finished.

## Usage

### Creating migrations

Migrations consist of a map of two one-argument functions, `:up` and
`:down`, and a unique identifier, `:id`.

Here's an example of one that modifies an in-memory atom:

    {:id "add-dog"
     :up   (fn [db] (swap! db conj :dog))
     :down (fn [db] (swap! db disj :dog))}

The `defmigration` macro is usually used to define migrations. It will
automatically add the `:id` key for you, based on the var name:

    (defmigration add-dog
      {:up   (fn [db] (swap! db conj :dog))
       :down (fn [db] (swap! db disj :dog))})

Because we're dealing with maps and functions, we can create factor
out common functionality into higher-level functions:

    (defn mconj [x]
      {:up   (fn [db] (swap! db conj x))
       :down (fn [db] (swap! db disj x))})

    (defmigration add-dog
      (mconj :dog))

You can list the migrations in a namespace with `list-migrations`:

    (list-migrations 'my.namespace.migrations)

The migrations will be returned in the order they were defined.
