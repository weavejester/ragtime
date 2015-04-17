(ns ragtime.sql.resources
  "Specify migrations as SQL resources.
  Useful when you'd like your migration scripts to be compiled into your jar."
  (:require [clojure.java.io :as io]
            [ragtime.sql.files :as rsf]))

(def ^:private default-dir "migrations")

(defn- id->resources [dir id]
  (->> (for [suffix [".down" ".up"]] (str dir "/" id suffix ".sql"))
       (map io/resource)))

(defn migrations
  "Returns a function that returns a list of migrations to apply.

  Since resources must be found on the classpath, they are not conveniently
  discoverable like regular files (at least, not without heriocs). Pass a list
  of resource names that should be considered as SQL migration files.

  All migration resources should live under a single directory. By default,
  this is set to 'migrations/', however you may optionally pass an alternative."
  ([ids]
    (migrations ids default-dir))
  ([ids dir]
    (fn []
      (let [resources (map (partial id->resources dir) ids)]
        (assert (every? identity resources)
                (str "Invalid migration resource names. "
                     "Make sure your names correspond to actual resources "
                     "on the classpath."))
        (rsf/files->migrations (map vector ids resources))))))
