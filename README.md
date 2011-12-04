# Ragtime

Ragtime is a Clojure library for migrating structured data, and
consists of ragtime.core, which is database-independent, and
ragtime.sql, which includes functions suitable for migration SQL
database schema.

## Installation

Add ragtime.core as a dependency if you want the database-independent
core:

    [ragtime/ragtime.core "0.1.0"]

The ragtime.sql library is currently not finished.

## Usage

Migrations consist of a map of two zero-argument functions, `:up` and
`:down`, and a unique identifier, `:id`.

Here's an example of one that modifies an in-memory atom:

    {:id "add-dog"
     :up   #(swap! my-pets conj :dog)
     :down #(swap! my-pets disj :dog)}

The `defmigration` macro is usually used to define migrations. It will
automatically add the `:id` key for you, based on the var name:

    (defmigration add-dog
      {:up   #(swap! my-pets conj :dog)
       :down #(swap! my-pets disj :dog)})

This also allows you to factor out common code:

    (defn conj-to-atom [a x]
      {:up   #(swap! a conj x)
       :down #(swap! a disj x)}

    (defmigration add-dog
      (conj-to-atom my-pets :dog))

You can list the migrations in a namespace with `list-migrations`:

    (list-migrations 'my.namespace.migrations)

The migrations will be returned in the order they were defined.
