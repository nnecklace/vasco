(ns vasco.riker.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [datomic.api :as d]))

(defn effect! [{:keys [conn]} kind data]
  (println "Performing effect" kind "with" data)
  (case kind
    :http/get (-> (client/request (apply merge {:method :get} data))
                  :body
                  (json/read-str :key-fn keyword))

    :db/transact @(d/transact conn data)

    nil))

(defn execute! [system {:plan/keys [effects]}]
  (println "Running effects" (prn-str effects))
  (doall
   (for [{:effect/keys [kind data]} effects]
     (effect! system kind data))))


(comment

  (doall
   (for [x (range 10)]
     x))

  (effect! {}
           :http/get
           [{:url "https://dummyjson.com/todos" :query-params {:limit 0}}])

  :-)
