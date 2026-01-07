(ns vasco.servitor.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [datomic.api :as d]
   [vasco.servitor.core :as sut]
   [vasco.utils-test :refer [with-test-db]]))

(deftest get-next-job
  (testing "Retrives the next job"
    (let [first-job-uuid (random-uuid)]
      (with-test-db [db [{:job/id first-job-uuid :job/task :task/foo :job/state :pending}
                         {:job/id (random-uuid) :job/task :task/foo :job/state :pending}
                         {:job/id (random-uuid) :job/task :task/bar :job/state :pending}]]
        (is (= first-job-uuid
               (-> (sut/get-next-job db :task/foo)
                   :job/id))))))

  (testing "Chooses the job that has waited the longest among pending"
    (let [oldest (random-uuid)
          newer  (random-uuid)]
      (with-test-db [db [{:job/id oldest :job/task :task/foo :job/state :pending}]]
        (let [db1 (:db-after (d/with db [{:job/id newer :job/task :task/foo :job/state :pending}]))]
          (is (= oldest
                 (-> (sut/get-next-job db1 :task/foo)
                     :job/id)))))))

  (testing "Chooses the oldest waited failed job among multiple failed"
    (let [failed-old (random-uuid)
          failed-new (random-uuid)]
      (with-test-db [db [{:job/id failed-old :job/task :task/foo :job/state :failed}]]
        (let [db1 (:db-after (d/with db [{:job/id failed-new :job/task :task/foo :job/state :failed}]))]
          (is (= failed-old
                 (-> (sut/get-next-job db1 :task/foo)
                     :job/id)))))))

  (testing "Ignores jobs from other tasks when selecting next job"
    (let [wanted (random-uuid)
          other  (random-uuid)]
      (with-test-db [db [{:job/id wanted :job/task :task/foo :job/state :pending}]]
        (let [db1 (:db-after (d/with db [{:job/id other :job/task :task/bar :job/state :failed}]))]
          (is (= wanted
                 (-> (sut/get-next-job db1 :task/foo)
                     :job/id))))))))
