(ns vasco.oracle.core
  (:require
   [clojure.spec.alpha :as s]
   [vasco.oracle.dispatcher :refer [Dispatcher] :as d]))

(s/def :question/answer any?)
(s/def :question/kind keyword?)

(s/def ::question (s/keys :req [:question/kind :question/answer]))

(defn invoke-question [system {:question/keys [context interceptor answer]} ctx]
  (when (and (keyword? context) (not (s/valid? context ctx)))
    (throw (ex-info "Ctx did not conform to the expected context" {:context (s/describe context) :ctx ctx})))

  ;; run all interceptors
  (answer system ctx))

(defn compute [computations]
  (->> (for [{:computation/keys [kind data]} computations]
         (case kind
           :computed/result data
           nil))
       first))

(defn define [& questions]
  (let [invalid (filter #(not (s/valid? ::question %)) questions)
        duplicates (->> questions (map :question/kind) frequencies (filter (fn [[_ v]] (< 1 v))) (map first))
        question-registry (->> questions (map (juxt :question/kind #(dissoc % :question/kind))) (into {}))]

    ;; this is really stupid.... or perhaps not?
    (when (seq invalid)
      (throw (ex-info "Invalid questions! All questions require a kind and an answer, the following did not conform to spec" {:invalid-questions questions})))

    (when (seq duplicates)
      (throw (ex-info "Duplicate questions! The following questions were defined more than once" {:duplicate-questions duplicates})))

    (reify Dispatcher
      (tell [_]
        (->> question-registry
             keys
             set))
      (reveal [_]
        (->> (for [[kind question] question-registry
                   :let [{:question/keys [answer interceptor context]} question]]
               [kind {:answer answer
                      :interceptor interceptor
                      :context (s/describe context)}])
             (into {})))
      (invoke [_ system params]
        (let [{:keys [kind ctx]} params
              question (kind question-registry)]
          (invoke-question system question ctx))))))

(defn consult [dispatcher system params]
  (let [computations (d/invoke dispatcher system params)]
    (compute computations)))

(defn run-validators [validators dispatcher request]
  (let [errors (keep (fn [validator]
                       (try
                         (validator dispatcher request)
                         (catch clojure.lang.ExceptionInfo e
                           {:message (ex-message e)
                            :error (ex-data e)})))
                     validators)]
    (when (seq errors)
      (throw (ex-info "Validation failed" {:validation-errors (vec errors)})))))

(defn wrap-handler [{:keys [route dispatcher validators]}]
  (fn [handler]
    (fn [{:keys [uri system params] :as request}]
      (run-validators validators dispatcher request)
      (if (= route uri)
        (consult dispatcher system params)
        (handler request)))))
