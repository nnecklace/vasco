(ns vasco.oracle.core
  (:require
   [clojure.spec.alpha :as s]
   [vasco.oracle.dispatcher :refer [Dispatcher] :as d]))

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

;; TODO: check for duplicate questions
(defn define [& questions]
  (let [question-registry (->> questions
                               (map (juxt :question/kind #(dissoc % :question/kind)))
                               (into {}))]
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
