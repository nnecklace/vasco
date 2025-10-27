(ns vasco.http.middlewares
  (:require [clojure.edn :as edn]
            [datomic.api :as d]))

(defn wrap-post-method [handler]
  (fn [request]
    (if (= (:request-method request) :post)
      (handler request)
      (throw (Exception. "Method not allowed")))))

(defn create-system [{:keys [conn] :as dependencies}]
  (assoc dependencies
         :now (java.time.Instant/now)
         :now-ld (java.time.LocalDate/now)
         :db (d/db conn)))

(defn wrap-system [handler dependencies]
  (fn [request]
    (handler (assoc request :system (create-system dependencies)))))

(defn wrap-parsed-request [handler]
  (fn [request]
    (handler (assoc request :params (edn/read-string (slurp (:body request)))))))

(defn wrap-result [handler]
  (fn [request]
    (try
      (let [response (handler request)]
        {:status 200
         :body (pr-str {:success? true
                        :result response})})
      (catch Exception e
        {:status 400
         :body (pr-str {:success? false
                        :result (str "Error: " (.getMessage e))})}))))
