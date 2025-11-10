(ns vasco.http.middlewares
  (:require
   [clojure.edn :as edn]
   [datomic.api :as d])
  (:import
   (clojure.lang ExceptionInfo)))

(defn wrap-post-method [handler]
  (fn [{:keys [request-method] :as request}]
    (if (= request-method :post)
      (handler request)
      (throw (ex-info "Method not allowed" {:method request-method})))))

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
      (catch ExceptionInfo e
        {:status 400
         :body (pr-str {:success? false
                        :result {:kind :controlled-exception
                                 :message (str "Error: " (ex-message e))
                                 :error (ex-data e)}})})
      (catch Exception e
        {:status 500
         :body (pr-str {:success? false
                        :result {:kind :uncontrolled-exception
                                 :message (str "Catasrophically failed: " (.getMessage e))
                                 :error (ex-data e)}})}))))
