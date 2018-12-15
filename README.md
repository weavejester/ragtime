# Ragtime

[![Build Status](https://travis-ci.org/weavejester/ragtime.svg?branch=master)](https://travis-ci.org/weavejester/ragtime)

Ragtime is a Clojure library for migrating structured data in a way
that's database independent. It defines a common interface for
expressing migrations, much like [Ring][] defines a common interface
for expressing web applications.

[ring]: https://github.com/ring-clojure/ring

## Installation

Add the following dependency to your project file:

    [ragtime "0.8.0"]

## Overview

Ragtime needs three pieces of data to work:

1. A migratable **data store**
2. An ordered sequence of **migrations**
3. A **strategy** on how to deal with conflicts

A data store is an implementation of the `DataStore` protocol, and
tells Ragtime how to record which migrations are applied to an
arbitrary store of data, such as a database.

Since 0.5.0, migrations are implementations of the `Migration` protocol,
which has three methods:

* `id`        - returns a unique ID for the migration
* `run-up!`   - applies the migration to a database
* `run-down!` - rolls back the migration in a database

Ragtime comes with a way of loading SQL migrations from files, and
applying them to a SQL database.

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

Copyright © 2018 James Reeves

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
