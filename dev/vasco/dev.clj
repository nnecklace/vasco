(ns vasco.dev
  (:require
   [integrant.repl :as repl]
   [vasco.system :as system]))

(defn start []
  (set! *print-namespace-maps* false)
  (repl/set-prep! (fn [] system/config))
  (repl/go))

(defn reset []
  (repl/reset))

(defn stop []
  (repl/halt))

(comment
  (start)
  (reset)
  (stop))
