## 0.8.0 (2018-12-15)

* Fixed SQL file migration IDs under windows (#121)
* Added `ragtime.strategy/ignore-future` (#129)
* Added support for turning off transactions in JDBC migrations (#124)

## 0.7.2 (2017-09-11)

* Fixed table creation when table has schema (#118)

## 0.7.1 (2017-04-02)

* Added `ragtime.jdbc.migrtion/create-table` function

## 0.7.0 (2017-03-28)

* Renamed `ragtime/ragtime.core` project to `ragtime/core`
* Renamed `ragtime/ragtime.jdbc` project to `ragtime/jdbc`
* Changed `ragtime.core` functions to use option maps
* Replaced `Exception` instances with more detailed `ex-info` exceptions
* Moved reporter callback from `ragtime.repl` to `ragtime.core`
* Fixed JDBC migrations to always sort by `:id` (#113)

## 0.6.4 (2017-02-15)

* Fixed condition check comparing migrations to discards (#114)
* Limited table check to connection catelog (#110)

## 0.6.3 (2016-08-15)

* Added support for DB spec with existing connection

## 0.6.2 (2016-08-11)

* Fixed database connection leak

## 0.6.1 (2016-06-29)

* Add check for existance of migration table

## 0.6.0 (2016-05-06)

* Updated clojure.java.jdbc to 0.5.8
