(ns vasco.system
  (:require
   [clojure.java.io :as io]
   [datomic.api :as d]
   [integrant.core :as ig]
   [vasco.http.server :as server]
   [vasco.http.handler :as http-handler]
   [vasco.servitor.core :as servitor]))

(defmethod ig/init-key :application/environment [_ _])

(defmethod ig/init-key :http/server [_ {:keys [config handler]}]
  (server/start handler config))

(defmethod ig/halt-key! :http/server [_ server]
  (server/stop server))

(defmethod ig/init-key :http/handler [_ {:keys [dependencies]}]
  (http-handler/router dependencies))

(defmethod ig/init-key :datomic/migrations [_ {:keys [config]}]
  config)

(defmethod ig/init-key :datomic/conn [_ conn-config]
  (println "Launching datomic with following config " (prn-str conn-config))
  (let [uri (str (:db/host conn-config) (:db/name conn-config))]
    (d/create-database uri)
    (d/connect uri)))

(defmethod ig/halt-key! :datomic/conn [_ conn]
  (d/release conn))

(defmethod ig/init-key :datomic/db [_ {:keys [conn]}]
  (fn [] (d/db conn)))

(defmethod ig/init-key :servitor/service [_ {:keys [services]}]
  (let [services (->> services
                      (filter :run?)
                      (map #(servitor/create-service (:id %) (:interval %))))]
    (doseq [service services]
      (servitor/start-service! service))
    services))

(defmethod ig/halt-key! :servitor/service [_ services]
  (doseq [service services]
    (servitor/stop-service! service)))

(def config
  (ig/read-string (slurp (io/resource "config.edn"))))
