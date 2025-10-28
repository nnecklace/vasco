(ns vasco.oracle.core
  (:require
   [vasco.oracle.questions.add :as add]
   [clojure.spec.alpha :as s]))

(defn valid-params? [params]
  (and (contains? params :kind) (contains? params :ctx)))

(defn prepare-answer [system {:question/keys [context interceptor answer]} ctx]
  (if (s/valid? context ctx)
    ;; run all interceptors
    ;; run handler
    (answer system ctx)
    (throw (Exception. "Ctx did not conform to the expected context"))))

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
                               (into {}))
        dispatcher
        (fn [system params]
          (let [{:keys [kind ctx]} params
                question (kind question-registry)]
            ;; TODO: This if is not needed
            (prepare-answer system question ctx)))]
    (with-meta dispatcher {::registry question-registry})))

(def dispatcher
  (define
    add/question))

(defn consult [dispatcher system params]
  (let [computations (dispatcher system params)]
    (compute computations)))

(defn reveal [dispatcher]
  (let [registry (::registry (meta dispatcher))]
    (->> (for [[kind question] registry
               :let [{:question/keys [answer interceptor context]} question]]
           [kind {:answer answer
                  :interceptor interceptor
                  :context (s/describe context)}])
         (into {}))))

(defn defined-questions [dispatcher]
  (->> dispatcher
       meta
       ::registry
       keys
       set))

(defn attach-handler [dispatcher]
  (fn [handler]
    (fn [{:keys [uri system params] :as request}]
      (if (= "/oracle" uri)
        (cond
          (not (s/valid? valid-params? params))
          (throw (Exception. "Request body did not conform to spec"))

          (not (contains? (defined-questions dispatcher) (:kind params)))
          (throw (Exception. "Invalid qestion"))

          :else
          (consult dispatcher system params))
        (handler request)))))

(comment
  (reveal dispatcher)
  (defined-questions dispatcher)

  )
