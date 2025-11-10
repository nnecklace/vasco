(ns vasco.http.server
  (:require
   [org.httpkit.server :as server]))

(defn start [handler {:keys [port] :as http-config}]
  (println (str "Running server on " port))
  (server/run-server handler http-config))

(defn stop [shutdown-server]
  (println "Shutting down http server")
  (shutdown-server))
