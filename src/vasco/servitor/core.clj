(ns vasco.servitor.core
  (:require
   [clojure.core.async :refer [alts! chan close! go-loop timeout]]
   [vasco.servitor.task :as task]
   [datomic.api :as d]))

(defprotocol Service
  (start! [_])
  (stop! [_]))

(defn count-active-jobs [db id]
  (d/q '[:find (count ?e) .
         :in $ ?type
         :where
         [?e :job/type ?type]
         [?e :job/state :pending]]
       db
       id))

;; A service should be tied to a database table
;; A task should be added to a database table
(defn create-service [conn {:keys [interval id]}]
  (let [stop-ch (chan)]
    (reify Service
      (start! [_]
        (println "Starting a servitor service")
        (go-loop []
          (let [db (d/db conn)
                job-count (count-active-jobs db id)]
            (println "Performing task " (name id) " in background! There are " job-count " active jobs queued")
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
