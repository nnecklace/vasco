(ns vasco.oracle.core
  (:require
   [vasco.oracle.answers.add :as add]
   [clojure.spec.alpha :as s]))

(defn question [question answer]
  {::question question
   ::answer answer})

(defn give-answer [system {:keys [handler middleware schema]} ctx]
  (if (s/valid? schema ctx)
    ;; chekc that context is valid according to schema
    ;; run all middlewares
    ;; run handler
    (handler system ctx)
    (throw (Exception. "Ctx did not conform to schema"))))

;; TODO: check for duplicate questions
(defn define [& questions]
  (let [question-registry (->> questions
                               (map (juxt ::question identity))
                               (into {}))
        dispatcher
        (fn [system params]
          (let [{:keys [question ctx]} params
                answer (some-> question-registry
                               question
                               ::answer)]
            (if answer
              (give-answer system answer ctx)
              (throw (Exception. "Invalid question")))))]
    (with-meta dispatcher {::registry question-registry})))

(def dispatcher
  (define
    (question :add add/answer)))

(defn describe [dispatcher]
  (let [registry (::registry (meta dispatcher))]
    (->> (for [[q {:keys [::answer]}] registry
               :let [{:keys [handler middleware schema]} answer]]
           [q {:handler handler
               :middleware middleware
               :schema (s/describe schema)}])
         (into {}))))

(comment
  (describe dispatcher))
