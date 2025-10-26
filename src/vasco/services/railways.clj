(ns vasco.services.railways)

(defn get [system]
  {:plan/id :railways/get
   :plan/effects
   [{:effect/kind :db/get
     :effect/data (:db system)}
    {:effect/kind :time/lookup
     :effect/data (:now-ld system)}]})
