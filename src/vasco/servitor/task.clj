(ns vasco.servitor.task)

(defn fetch-trains! []
  (println "Fetching trains!"))

(defn fetch-map! []
  (println "Fetching map!"))

(def registered-tasks
  [[::fetch-trains #'fetch-trains!]
   [::fetch-map #'fetch-map!]])

(defn do! [task]
  (let [task-runner (->> registered-tasks
                         (filter #(= task (first %)))
                         (first)
                         (second))]
    (task-runner)))
