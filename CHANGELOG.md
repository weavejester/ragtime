## 0.9.5 (2024-11-06)

* Added primary key restriction to migrations table
* Added `:migrations-table-exists-sql` option to jdbc and next-jdbc
* Updated next.jdbc dependency to 1.3.955

## 0.9.4 (2024-03-01)

* Fixed SQL separator marker for MySQL (#159)

## 0.9.3 (2022-11-15)

* Made `migrate-all` interruptable (#154)

## 0.9.2 (2022-06-15)

* Fixed incompatibility with next.jdbc 1.2.772 and later (#152)

## 0.9.1 (2022-02-09)

* Fixed table lookup with quoted identifiers (#150)

## 0.9.0 (2021-10-28)

* Changed project group name to dev.weavejester
* Added library to support next.jdbc (#147)

## 0.8.1 (2021-03-05)

* Fixed reflection warnings (#139)
* Fixed table metadata resultset not being closed (#143)
* Fixed test memory database ordering (#138)

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
