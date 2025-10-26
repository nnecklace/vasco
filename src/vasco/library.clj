(ns vasco.library
  (:require
   [vasco.services.railways :as railways]))

(def catalog
  [{:entry/kind :entries/railways
    :entry/plan #'railways/get}])

(defn open [system entry]
  (let [entry-plan (:entry/plan entry)]
    (entry-plan system)))

(defn read [system entry-request]
  (some #(when (= (:kind entry-request) (:entry/kind %)) (open system %)) catalog))
