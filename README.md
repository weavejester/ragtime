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

## Usage

Ragtime needs three pieces of data to work:

1. A migratable **database** connection
2. An ordered sequence of **migrations**
3. A **strategy** on how to deal with conflicts

We can declare these in a map for a JDBC SQL database:

```clojure
(require '[ragtime.repl :as repl]
         '[ragtime.jdbc :as jdbc]
         '[ragtime.strategy :as strategy])

(def config
  {:database   (jdbc/sql-database {:connection-uri "jdbc:h2:file:example.h2"})
   :migrations (jdbc/load-directory "migrations")
   :strategy   strategy/rebase})
```

This configuration will attempt to use a [H2][] database called
"example.h2", and look for migrations in a directory called
"migrations".

To migrate the migrations, we can use:

```clojure
(repl/migrate config)
```

And to rollback a migration:

```clojure
(repl/rollback config)
```

[h2]: http://www.h2database.com/html/main.html


## Documentation

* [Wiki](https://github.com/weavejester/ragtime/wiki)
* [API docs](http://weavejester.github.com/ragtime)

## License

Copyright © 2015 James Reeves

Distributed under the Eclipse Public License, the same as Clojure.
