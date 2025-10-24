(ns vasco.library
  (:require
   [vasco.services.railways :as railways]))

(def catalog
  [{:entry/kind :entries/railways
    :entry/plan #'railways/get}])

#_(defn resolve-dependencies [system dependencies]
  (->> dependencies
       (map
        (fn [dep]
          (cond
            (= :system/db dep)
            [:db (:db system)]

            (= :time/now-ld dep)
            [:now-ld (java.time.LocalDate/now)])))
       (into {})))

(defn open [system entry]
  (let [entry-plan (:entry/plan entry)
        #_#_dependencies (resolve-dependencies system (:entry/dependencies entry))]
    (entry-plan system)))

(defn read [system entry-request]
  (some #(when (= (:kind entry-request) (:entry/kind %)) (open system %)) catalog))

(comment

  (read {:db {:db/rows [1 2 3]}} {:kind :entries/railways})

  )
