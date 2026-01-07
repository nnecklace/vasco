(ns vasco.servitor.core
  (:require
   [clojure.core.async :refer [alts! chan close! go-loop timeout]]
   [datomic.api :as d]))

(defprotocol Service
  (start! [_])
  (stop! [_]))

(defn get-next-job [db task]
  (->> (d/q '[:find [?e (min ?tx)]
              :in $ ?task
              :where
              [?e :job/id _ ?tx]
              [?e :job/task ?task]
              (or
               [?e :job/state :failed]
               [?e :job/state :pending])
              [?tx :db/txInstant _]]
            db
            task)
       (first)
       (d/entity db)))

(defn create-service [conn {:keys [interval task]}]
  (let [stop-ch (chan)]
    (reify Service
      (start! [_]
        (println "Starting a servitor service")
        (go-loop []
          (let [db (d/db conn)
                job (get-next-job db task)]
            (println "Performing task" (name job) "in background! There are" job "active jobs queued")
            (let [[_ ch] (alts! [(timeout (+ interval (rand-int 1000))) stop-ch])]
              (when-not (= ch stop-ch)
                (recur))))))
      (stop! [_]
        (println "Closing servitor service")
        (close! stop-ch)))))

(defn start-service! [service]
  (start! service))

(defn stop-service! [service]
  (stop! service))
