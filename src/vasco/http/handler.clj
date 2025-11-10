(ns vasco.http.handler
  (:require
   [vasco.oracle.handler :as oracle-handler]
   [vasco.http.middlewares :as middlewares]))

;; TODO: Make this 404 handler
(defn not-found-handler [request]
  (throw (ex-info "Route not found" {:uri (:uri request)})))

(defn router [dependencies]
  (-> not-found-handler
      oracle-handler/handler
      (middlewares/wrap-system dependencies)
      (middlewares/wrap-parsed-request)
      (middlewares/wrap-post-method)
      (middlewares/wrap-result)))
