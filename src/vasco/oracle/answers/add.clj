(ns vasco.oracle.answers.add
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::ctx number?)

(defn handler [system ctx]
  (+ 10 ctx))

;; TODO: These keys should probably be namespaced
(def answer
  {:handler #'handler
   :middleware #{:auth}
   :schema ::ctx})

(comment

  (::s/problems (s/explain-data ::ctx "25"))

  )
