(ns vasco.servitor.task)

(defn fetch-trains! []
  ;; fetches trains
  (println "Fetching trains!")
  )

(def registered-tasks
  [[::fetch-trains #'fetch-trains!]])

(defn do! [task]
  (let [task-runner (->> registered-tasks
                         (filter #(= task (first %)))
                         (first)
                         (second))]
    (task-runner)))
