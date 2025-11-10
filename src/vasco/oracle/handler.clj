(ns vasco.oracle.handler
  (:require
   [vasco.oracle.core :as oracle]
   [vasco.oracle.questions.add :as add]
   [vasco.oracle.validator :as validator]
   [vasco.oracle.dispatcher :as d]))

;; TODO: updating the dispatcher will require running dev/reset for changes to take affect
(def dispatcher
  (oracle/define
    add/question))

(def handler
  (oracle/wrap-handler {:route "/oracle"
                        :dispatcher dispatcher
                        :validators [validator/validate-question-kind]}))

(comment
  (d/reveal dispatcher)
  (d/tell dispatcher)
  )
