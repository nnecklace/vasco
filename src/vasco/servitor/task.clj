(ns vasco.servitor.task
  (:require
   [clj-http.client :as client]
   [clojure.data.json :as json]
   [vasco.riker.plans.todos :as todos]
   [vasco.riker.core :as riker]))

(defn init-todos [opts conn]
  (try
    (let [todos (:todos (first (riker/execute! {:conn conn} (todos/fetch-plan opts))))]
      (println "Todos found " (prn-str todos))
      (riker/execute! {:conn conn} (todos/transaction-plan todos)))
    (catch Exception _
      (println "Something went wrong"))))

(def registered-tasks
  {::init-todos #'init-todos})

(comment
  ;; fetch todos
  (-> (client/request {:method :get
                       :url "https://dummyjson.com/todos"
                       :query-params {:limit 0}})
      :body
      (json/read-str :key-fn keyword)
      :todos)

  (-> (client/request (apply merge {:method :get} [{:url "https://dummyjson.com/todos" :query-params {:limit 0}}]))
      :body
      (json/read-str :key-fn keyword)
      :todos)

  :-)
