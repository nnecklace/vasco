(ns vasco.oracle.core
  (:require
   [vasco.oracle.answers.add :as add]
   [clojure.spec.alpha :as s]))

(defn prepare-answer [system {:question/keys [context interceptor answer]} ctx]
  (if (s/valid? context ctx)
    ;; chekc that context is valid according to schema
    ;; run all middlewares
    ;; run handler
    (answer system ctx)
    (throw (Exception. "Ctx did not conform to the expected context"))))

;; TODO: check for duplicate questions
(defn define [& questions]
  (let [question-registry (->> questions
                               (map (juxt :question/kind #(dissoc % :question/kind)))
                               (into {}))
        consult-fn
        (fn [system params]
          (let [{:keys [kind ctx]} params
                ;; TODO: does this and need to be here?
                question (and kind (kind question-registry))]
            (if question
              (prepare-answer system question ctx)
              (throw (Exception. "Invalid question")))))]
    (with-meta consult-fn {::registry question-registry})))

(def consult
  (define
    add/question))

(defn reveal [consult-fn]
  (let [registry (::registry (meta consult-fn))]
    (->> (for [[kind question] registry
               :let [{:question/keys [answer interceptor context]} question]]
           [kind {:answer answer
                  :interceptor interceptor
                  :context (s/describe context)}])
         (into {}))))

(comment
  (reveal consult))
