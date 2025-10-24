(ns vasco.servitor.core
  (:require
   [clojure.core.async :refer [alts! chan close! go-loop timeout]]
   [vasco.servitor.task :as task]))

(defprotocol Service
  (start! [_])
  (stop! [_]))

(defn create-service [task interval]
  (let [stop-ch (chan)]
    (reify Service
      (start! [_]
        (println "Starting a servitor service")
        (go-loop []
          (println "Performing task " (name task) " in background!")
          (task/do! task)
          (let [[_ ch] (alts! [(timeout (+ interval (rand-int 1000))) stop-ch])]
            (when-not (= ch stop-ch)
              (recur)))))
      (stop! [_]
        (println "Closing servitor service")
        (close! stop-ch)))))

(defn start-service! [service]
  (start! service))

(defn stop-service! [service]
  (stop! service))
