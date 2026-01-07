(ns vasco.system
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [datomic.api :as d]
   [integrant.core :as ig]
   [vasco.http.handler :as http-handler]
   [vasco.http.server :as server]
   [vasco.servitor.core :as servitor]))

;; TODO:
;; 0. Introduce tests
;; 1. Create a database for the servitor services
;; 2. Each service polls their own database
;; 3. Introduce structured logging
;; 4. Introduce commands

(defmethod ig/init-key :application/environment [_ env]
  env)

(defmethod ig/init-key :http/server [_ {:keys [config handler]}]
  (server/start handler config))

(defmethod ig/halt-key! :http/server [_ server]
  (server/stop server))

(defmethod ig/init-key :http/handler [_ {:keys [dependencies]}]
  (http-handler/router dependencies))

(defmethod ig/init-key :datomic/migrations [_ {:keys [conn location]}]
  (let [migrations (edn/read-string (slurp (io/resource location)))]
    (println "Running migrations")
    @(d/transact conn migrations)))

(defmethod ig/init-key :datomic/conn [_ conn-config]
  (println "Launching datomic with following config " (prn-str conn-config))
  (let [uri (str (:db/host conn-config) (:db/name conn-config))]
    (d/create-database uri)
    (d/connect uri)))

(defmethod ig/halt-key! :datomic/conn [_ conn]
  (d/release conn))

(defmethod ig/init-key :servitor/config [_ {:keys [conn services]}]
  (let [services (->> services
                      (filter :run?)
                      (map #(servitor/create-service conn %)))]
    (doseq [service services]
      (servitor/start-service! service))
    services))

(defmethod ig/halt-key! :servitor/config [_ services]
  (doseq [service services]
    (servitor/stop-service! service)))

(def config
  (ig/read-string (slurp (io/resource "config.edn"))))

(comment

  (require '[integrant.repl.state])

  (def system integrant.repl.state/system)

  (def conn (:datomic/conn system))

  (def db (d/db conn))

  (d/q '[:find (count ?eid) .
         :where [?eid :job/id ?id]]
       db)

  @(d/transact conn [[:db/add "new-job" :job/id #uuid "d1c2b5e2-2d77-4b05-9dd4-6e8b578247ca"]])

  @(d/transact conn [[:db/add 17592186045418 :job/task ::test-job]])

  (d/touch (d/entity db 17592186045418))

  @(d/transact conn [{:job/id (java.util.UUID/randomUUID)
                      :job/task :vasco.servitor.task/fetch-todos
                      :job/state :failed
                      :job/retries 5}])

  (d/q '[:find (count ?e) .
         :where
         [?e :job/id ?id]
         [?e :job/state :failed]]
       db)

  (d/touch (d/entity db 17592186045421))

  :rcf)
