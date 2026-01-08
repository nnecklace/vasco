(ns vasco.riker.plans.todos)

(defn fetch-plan [{:keys [url limit]}]
  {:plan/effects
   [{:effect/kind :http/get
     :effect/data [{:url url
                    :query-params {:limit limit}}]}]})

(defn transaction-plan [todos]
  {:plan/effects
   [{:effect/kind :db/transact
     :effect/data (mapv
                   (fn [{:keys [id todo completed]}]
                     {:todo/id id
                      :todo/task todo
                      :todo/done? completed})
                   (take 10 todos))}]})
