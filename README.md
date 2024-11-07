# Ragtime [![Build Status](https://github.com/weavejester/ragtime/actions/workflows/test.yml/badge.svg)](https://github.com/weavejester/ragtime/actions/workflows/test.yml)

Ragtime is a Clojure library for migrating structured data in a way
that's database independent. It defines a common interface for
expressing migrations, much like [Ring][] defines a common interface
for expressing web applications.

[ring]: https://github.com/ring-clojure/ring

## Installation

Add the following dependency to your deps.edn file:

    dev.weavejester/ragtime {:mvn/version "0.9.5"}

Or to your Leiningen project file:

    [dev.weavejester/ragtime "0.9.5"]

## Overview

Ragtime needs three pieces of data to work:

1. A migratable **data store**
2. An ordered sequence of **migrations**
3. A **strategy** on how to deal with conflicts

A data store is an implementation of the `DataStore` protocol, and
tells Ragtime how to record which migrations are applied to an
arbitrary store of data, such as a database. It has three methods:

* `add-migration-id`      - add a new migration ID to the store
* `remove-migration-id`   - remove a migration ID from the store
* `applied-migration-ids` - return an ordered list of applied IDS

Migrations are implementations of the `Migration` protocol, which also
has three methods:

* `id`        - returns a unique ID for the migration
* `run-up!`   - applies the migration to a database
* `run-down!` - rolls back the migration in a database

Ragtime comes with implementations of these protocols for Cloure's
[java.jdbc][] library and Sean Corfield's [next.jdbc][] library,
therefore supporting SQL migrations out of the box.

The migration store for SQL database is a special migrations table,
and the migrations can be specified as either `.sql` files, or `.edn`
files. For more information, see the documentation below:

[java.jdbc]: https://github.com/clojure/java.jdbc
[next.jdbc]: https://github.com/seancorfield/next-jdbc

## Documentation

* [Getting Started](https://github.com/weavejester/ragtime/wiki/Getting-Started)
* [Upgrading from 0.3.x](https://github.com/weavejester/ragtime/wiki/Upgrading-from-0.3.x)
* [Upgrading from 0.4.x](https://github.com/weavejester/ragtime/wiki/Upgrading-from-0.4.x)
* [Concepts](https://github.com/weavejester/ragtime/wiki/Concepts)
* [SQL Migrations](https://github.com/weavejester/ragtime/wiki/SQL-Migrations)
* [Leiningen Integration](https://github.com/weavejester/ragtime/wiki/Leiningen-Integration)
* [API docs](http://weavejester.github.io/ragtime)
* [Third-party Libraries](https://github.com/weavejester/ragtime/wiki/Third-party-Libraries)

## License

Copyright © 2024 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version
