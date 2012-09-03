# Ragtime

Ragtime is a Clojure library for migrating structured data. It defines
a common interface for expressing migrations, much like [Ring][1]
defines a common interface for expression web applications.

[1]: https://github.com/ring-clojure/ring

## Libraries

* ragtime.core -
  database independent tools and functions for managing migrations

* ragtime.sql -
  an adapter for applying migrations to a SQL database
  
* ragtime.sql.files -
  provides a way of specifying migrations as SQL script files

* ragtime.lein -
  a Leiningen plugin that wraps ragtime.core

## Installation

Add ragtime.core as a dependency if you just want the database-
independent core:

```clojure
:dependencies [[ragtime/ragtime.core "0.3.0-SNAPSHOT"]]
```

Or add the full library if you want support for SQL databases:

```clojure
:dependencies [[ragtime "0.3.0-SNAPSHOT"]]
```

If you want to integrate Ragtime into Leiningen:

```clojure
:plugins [[ragtime/ragtime.lein "0.3.0-SNAPSHOT"]]
```

## Usage

### Migrations

Migrations consist of a map of two one-argument functions, `:up` and
`:down`, and a unique identifier, `:id`.

Here's an example of one that modifies an in-memory atom stored in
`(:data db)`:

    (def add-dog
      {:id "add-dog"
       :up   (fn [db] (swap! (:data db) conj :dog))
       :down (fn [db] (swap! (:data db) disj :dog))})

You can apply a migration to a database using the `migrate` function,
and remove a migration using `rollback`. Ragtime will maintain a list
of applied migrations which you can access with the
`applied-migrations` function:

    (migrate db add-dog)

    (applied-migrations db)
    => [add-dog]

    (rollback db add-dog)

    (applied-migrations db)
    => []

### Databases

The database itself needs to be a type that implements the
`ragtime.core/Migratable` protocol. If we wanted an in-memory database
that wrapped an atom, we might implement it as a record:

    (defrecord MemoryDatabase [data]
      (add-migration-id [_ id]
        (swap! data update-in [:migrations] conj id))
      (remove-migration-id [_ id]
        (swap! data update-in [:migrations]
               (vec (remove (partial = id) %))))
      (applied-migration-ids [_]
        (seq (:migrations @data))))

    (defn memory-database []
      (MemoryDatabase. (atom {:migrations []})))

It's important that `applied-migration-ids` maintains the order
migrations were applied in, which is why we're using a vector in this
example.

The ragtime.sql library includes a `SqlDatabase` record that can be used
to wrap a database connection map. For instance:

    #ragtime.sql.database.SqlDatabase{
      :classname "org.h2.Driver"
      :subprotocol "h2"
      :subname "mem:test_db"
      :user "test"
      :password ""}
