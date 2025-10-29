(ns vasco.http.handler
  (:require
   [vasco.oracle.core :as oracle]
   [vasco.http.middlewares :as middlewares]))

;; TODO: Make this 404 handler
(defn handler [request]
  (let [{:keys [uri]} request]
    (cond
      (= uri "/riker")
      "Riker!"

      :else
      (throw (Exception. "Route not found")))))

(defn router [dependencies]
  (-> handler
      oracle/handler
      (middlewares/wrap-system dependencies)
      (middlewares/wrap-parsed-request)
      (middlewares/wrap-post-method)
      (middlewares/wrap-result)))
