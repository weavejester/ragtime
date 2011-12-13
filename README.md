# Ragtime

Ragtime is a Clojure library for migrating structured data, and
consists of ragtime.core, which is database-independent, and
ragtime.sql, which includes functions suitable for migration SQL
database schema.

Ragtime defines a common interface for expressing migrations, but not
how those migrations are generated. Think of it as "Ring for migrations".

## Installation

Add ragtime.core as a dependency if you just want the database-
independent core:

    [ragtime/ragtime.core "0.2.0"]

Or add the full library if you want support for SQL databases:

    [ragtime "0.2.0"]

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
