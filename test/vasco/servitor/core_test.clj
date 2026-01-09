(ns vasco.servitor.core-test
  (:require
   [clj-http.client :as client]
   [clojure.test :refer [deftest is testing]]
   [datomic.api :as d]
   [vasco.servitor.core :as sut]
   [vasco.utils-test :refer [with-conn with-db]]))

(deftest ^:integration todo-service
  (testing "integration successful"
    (let [uuid (random-uuid)]
      (with-conn [conn [{:job/id uuid
                         :job/task :vasco.servitor.task/init-todos
                         :job/state :pending
                         :job/retries 5}]]
        (let [service (sut/create-service conn {:interval 2000
                                                :task :vasco.servitor.task/init-todos
                                                :opts {:url "https://dummyjson.com/todos"
                                                       :limit 0}})]
          (sut/start-service! service)
          (Thread/sleep 3000)
          (sut/stop-service! service)

          (is (= 1 (d/q '[:find (count ?e) .
                          :in $ ?id
                          :where
                          [?e :job/id ?id]
                          [?e :job/state :succeeded]]
                        (d/db conn)
                        uuid)))))))

  (testing "integration failed"
    (let [uuid (random-uuid)]
      (with-conn [conn [{:job/id uuid
                         :job/task :vasco.servitor.task/init-todos
                         :job/state :pending
                         :job/retries 5}]]
        (with-redefs [client/request (fn [& _]  (throw (Exception. "my exception message")))]
          (let [service (sut/create-service conn {:interval 2000
                                                  :task :vasco.servitor.task/init-todos
                                                  :opts {:url "https://dummyjson.com/todos"
                                                         :limit 0}})]
            (sut/start-service! service)
            (Thread/sleep 3000)
            (sut/stop-service! service)

            (is (= 1 (d/q '[:find (count ?e) .
                            :in $ ?id
                            :where
                            [?e :job/id ?id]
                            [?e :job/state :failed]]
                          (d/db conn)
                          uuid)))))))))

(deftest get-next-job
  (testing "Retrives the next job"
    (let [first-job-uuid (random-uuid)]
      (with-db [db [{:job/id first-job-uuid :job/task :task/foo :job/state :pending}
                    {:job/id (random-uuid) :job/task :task/foo :job/state :pending}
                    {:job/id (random-uuid) :job/task :task/bar :job/state :pending}]]
        (is (= first-job-uuid
               (-> (sut/get-next-job db :task/foo)
                   :job/id))))))

  (testing "Chooses the job that has waited the longest among pending"
    (let [oldest (random-uuid)
          newer  (random-uuid)]
      (with-db [db [{:job/id oldest :job/task :task/foo :job/state :pending}]]
        (let [db1 (:db-after (d/with db [{:job/id newer :job/task :task/foo :job/state :pending}]))]
          (is (= oldest
                 (-> (sut/get-next-job db1 :task/foo)
                     :job/id)))))))

  (testing "Chooses the oldest waited failed job among multiple failed"
    (let [failed-old (random-uuid)
          failed-new (random-uuid)]
      (with-db [db [{:job/id failed-old :job/task :task/foo :job/state :failed}]]
        (let [db1 (:db-after (d/with db [{:job/id failed-new :job/task :task/foo :job/state :failed}]))]
          (is (= failed-old
                 (-> (sut/get-next-job db1 :task/foo)
                     :job/id)))))))

  (testing "Ignores jobs from other tasks when selecting next job"
    (let [wanted (random-uuid)
          other  (random-uuid)]
      (with-db [db [{:job/id wanted :job/task :task/foo :job/state :pending}]]
        (let [db1 (:db-after (d/with db [{:job/id other :job/task :task/bar :job/state :failed}]))]
          (is (= wanted
                 (-> (sut/get-next-job db1 :task/foo)
                     :job/id))))))))
