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

## Documentation

* [Wiki](https://github.com/weavejester/ragtime/wiki)

## License

Copyright © 2012 James Reeves

Distributed under the Eclipse Public License, the same as Clojure.
