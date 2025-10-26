(ns vasco.http.handler
  (:require
   [vasco.oracle.core :as oracle]
   [vasco.http.middlewares :as middlewares]))

(defn handler [request]
  (let [{:keys [uri params system]} request]
    (cond
      (= uri "/oracle")
      (oracle/dispatcher system params)

      (= uri "/riker")
      "Riker!"

      :else
      (throw (Exception. "Route not found")))))

(defn router [dependencies]
  (-> handler
      (middlewares/wrap-result)
      (middlewares/wrap-system dependencies)
      (middlewares/wrap-parsed-request)
      (middlewares/wrap-post-method)))
