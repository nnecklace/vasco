(ns vasco.services.railways)

(defn get [system]
  {:plan/id :railways/get
   :plan/effects
   [{:effect/kind :db/get
     :effect/data (d/db (:db/conn system))}
    {:effect/kind :time/lookup
     :effect/data (:time/now-ld system)}]})
