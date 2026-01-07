(ns vasco.servitor.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [vasco.servitor.core :as sut]
   [vasco.utils-test :refer [with-test-db]]))

(deftest get-next-job
  (testing "Retrives the next job"
    (let [first-job-uuid (random-uuid)]
      (with-test-db [db [{:job/id first-job-uuid :job/task :task/foo :job/state :pending}
                         {:job/id (random-uuid) :job/task :task/foo :job/state :pending}
                         {:job/id (random-uuid) :job/task :task/bar :job/state :pending}]]
        (is (= first-job-uuid
               (->> (sut/get-next-job db :task/foo)
                    :job/id)))))))
