(ns vasco.servitor.core-test
  (:require [vasco.servitor.core :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest servitor-core
  (testing "core functionality"
    (is (= 1 1))))
