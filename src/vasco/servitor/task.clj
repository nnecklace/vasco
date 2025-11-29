(ns vasco.servitor.task
  (:require
   [clj-http.client :as client]
   [clojure.data.json :as json]))

(defn fetch-todos! []
  (println "Fetching trains!"))

(def registered-tasks
  {::fetch-todos #'fetch-todos!})

(defn do! [task]
  (when-let [task-runner (some-> task registered-tasks)]
    (task-runner)))

(comment

  (-> "https://dummyjson.com/todos"
      (client/get {:query-params {:limit 0}})
      :body
      (json/read-str :key-fn keyword)
      :todos))
