(ns vasco.oracle.core
  (:require
   [vasco.oracle.questions.add :as add]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(defn invoke-question [system {:question/keys [context interceptor answer]} ctx]
  (if (s/valid? context ctx)
    ;; run all interceptors
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
            (invoke-question system question ctx)))]
    (with-meta dispatcher {::registry question-registry})))

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

;; TODO: think of better way of passing errors
(defn run-validators [validators dispatcher request]
  (let [errors (keep (fn [validator]
                       (try
                         (validator dispatcher request)
                         (catch Exception e
                           (.getMessage e))))
                     validators)]
    (when (seq errors)
      (throw (Exception. (str "Validation failed with " (str/join ", " errors)))))))

(defn wrap-handler [{:keys [route dispatcher validators]}]
  (fn [handler]
    (fn [{:keys [uri system params] :as request}]
      (run-validators validators dispatcher request)
      (if (= route uri)
        (consult dispatcher system params)
        (handler request)))))

(defn validate-body [_ {:keys [params]}]
  (when-not (and (contains? params :kind) (contains? params :ctx))
    (throw (Exception. "Request body did not conform to spec"))))

(defn validate-question-kind [dispatcher {:keys [params]}]
  (when-not (contains? (defined-questions dispatcher) (:kind params))
    (throw (Exception. "Invalid question kind"))))

;; TODO: updating the dispatcher will require running dev/reset for changes to take affect
(def dispatcher
  (define
    add/question))

(def handler
  (wrap-handler {:route "/oracle"
                 :dispatcher dispatcher
                 :validators [validate-body validate-question-kind]}))

(comment
  (reveal dispatcher)
  (defined-questions dispatcher)
  )
