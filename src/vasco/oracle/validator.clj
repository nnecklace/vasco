(ns vasco.oracle.validator
  (:require [vasco.oracle.dispatcher :as d]
            [clojure.spec.alpha :as s]));; TODO: d is a bad name, already used by datomic

(s/def ::body (s/keys :req [:kind]))

(defn validate-question-kind [dispatcher {:keys [params]}]
  (let [available-questions (d/tell dispatcher)]
    (cond
      (not (s/valid? ::body params))
      (throw (ex-info "Questions require a kind, kind was missing" {:defined-questions available-questions}))

      (not (contains? available-questions (:kind params)))
      (throw (ex-info "Invalid question kind" {:kind (:kind params) :defined-questions available-questions})))))
