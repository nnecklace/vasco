(ns vasco.servitor.core
  (:require
   [clojure.core.async :refer [alts! chan close! go-loop timeout]]
   [datomic.api :as d]
   [vasco.servitor.task :as t]))

(defprotocol Service
  (start! [_])
  (stop! [_]))

(defn get-next-job [db task]
  (->> (d/q '[:find ?e .
              :in $ ?task
              :where
              [?e :job/id _ ?tx]
              [(min ?tx) ?min-tx]
              [?e :job/task ?task]
              (or
               [?e :job/state :failed]
               [?e :job/state :pending])
              [(= ?tx ?min-tx)]]
            db
            task)
       (d/entity db)))

(defn create-service [conn {:keys [interval task opts]}]
  (let [stop-ch (chan)]
    (reify Service
      (start! [_]
        (println "Starting a servitor service")
        (go-loop []
          (let [db (d/db conn)]
            (println "Polling for" (name task))
            (when-let [job (get-next-job db task)]
              (println "Performing task" (name (:job/task job)) "in background!")
              (when-let [task-fn (t/registered-tasks (:job/task job))]
                (println "Running task-fn" (name (:job/task job)))
                (try
                  (task-fn opts conn)
                  (println "Task" (name (:job/task job)) "succeeded")
                  @(d/transact conn [[:db/add (:db/id job) :job/state :succeeded]])
                  (catch Exception _
                    (println "Task" (name (:job/task job)) "failed")
                    @(d/transact conn [[:db/add (:db/id job) :job/state :failed]])))))
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
