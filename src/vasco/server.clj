(ns vasco.server
  (:require
   [org.httpkit.server :as server])
  (:import
   (java.time Instant LocalDate)))

(defn start [handler {:keys [port] :as http-config}]
  (println (str "Running server on " port))
  (server/run-server handler http-config))

(defn create-system [{:keys [db] :as dependencies}]
  (assoc dependencies
         :now (Instant/now)
         :now-ld (LocalDate/now)
         :db (db)))

(defn handler [dependencies]
  (fn [request]
    (let [{:keys [uri request-method]} request
          system (create-system dependencies)]
      (cond
        (and (= uri "/oracle") (= request-method :post))
        {:status 200
         :body "Oracle!"}

        (and (= uri "/riker") (= request-method :post))
        {:status 200
         :body "Riker!"}

        :else
        {:status 404
         :body nil
         :headers {}}))))

(defn stop [stop-fn]
  (println "Shutting down http server")
  (stop-fn))
