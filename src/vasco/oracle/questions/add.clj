(ns vasco.oracle.questions.add
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::ctx number?)

(defn answer [_ ctx]
  [{:computation/kind :computed/result
    :computation/data (+ 10 ctx)}])

(def question
  {:question/kind :questions/add
   :question/answer #'answer
   :question/interceptor #{:auth}
   :question/context ::ctx})

(comment
  (::s/problems (s/explain-data ::ctx "25"))
  )
