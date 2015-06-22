# Ragtime

[![Build Status](https://travis-ci.org/weavejester/ragtime.svg?branch=dev)](https://travis-ci.org/weavejester/ragtime)

Ragtime is a Clojure library for migrating structured data in a way
that's database independent. It defines a common interface for
expressing migrations, much like [Ring][] defines a common interface
for expressing web applications.

[ring]: https://github.com/ring-clojure/ring

## Installation

Add the following dependency to your project file:

    [ragtime "0.4.0-SNAPSHOT"]

## Overview

Ragtime needs three pieces of data to work:

1. A migratable **database** connection
2. An ordered sequence of **migrations**
3. A **strategy** on how to deal with conflicts

Migrations are maps that contain three keys:

* `:id`   - a unique ID for the migration
* `:up`   - a function that applies the migration to a database
* `:down` - a function that rolls back the migration in a database

Ragtime comes with a way of loading SQL migrations from files, and
applying them to a SQL database.

## Documentation

* [Wiki](https://github.com/weavejester/ragtime/wiki)
* [API docs](http://weavejester.github.com/ragtime)

## License

Copyright © 2015 James Reeves

Distributed under the Eclipse Public License, the same as Clojure.
