(ns automaton-build.core-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.core :as sut]))

(deftest get-task
  (testing "Existing tasks is found"
    (is (= {:cli-params-mode :none,
            :exec-task identity}
           (sut/get-task {"task-stub" {:cli-params-mode :none
                                       :exec-task identity}}
                         "task-stub"))))
  (testing "Non existing tasks is detected"
    (is (thrown-with-msg? Exception
                          #"Don't know that task"
                          (sut/get-task {}
                                        "non-existing-task"))))
  (testing "Missing bb-validation-method is found"
    (is (thrown-with-msg? Exception
                          #"Don't know how to execute that task"
                          (sut/get-task {"task-stub" {:cli-params-mode :none}}
                                        "task-stub")))))

(deftest create-apps-test
  (testing "Create-apps for build app is well formed"
    (let [build-apps (sut/create-apps)]
      (is (> (count build-apps)
             0))
      (is (map?
           (first build-apps))))))
