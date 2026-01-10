(ns vasco.servitor.task
  (:require
   [vasco.riker.plans.todos :as todos]
   [vasco.riker.core :as riker]))

(defn init-todos! [opts conn]
  (let [todos (:todos (first (riker/execute! {:conn conn} (todos/fetch-plan opts))))]
    (println "Todos found " (prn-str todos))
    (riker/execute! {:conn conn} (todos/transaction-plan todos))))

(def registered-tasks
  {::init-todos #'init-todos!})
