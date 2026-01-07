(ns vasco.utils-test
  (:require
   [clojure.java.io :as io]
   [datomic.api :as d]))

(defn create-database [uri schema]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    @(d/transact conn schema)
    conn))

(defn create-test-db [tx]
  (let [uri (str "datomic:mem://" (random-uuid))
        schema (read-string (slurp (io/resource "migrations.edn")))
        conn (create-database uri schema)]
    @(d/transact conn tx)
    {:conn conn
     :db (d/db conn)}))

(defmacro with-test-db
  {:clj-kondo/lint-as 'clojure.core/let}
  [[db-sym tx] & body]
  `(let [m# (create-test-db ~tx)
         ~db-sym (:db m#)
         res# (do ~@body)]
     (d/release (:conn m#))
     res#))
