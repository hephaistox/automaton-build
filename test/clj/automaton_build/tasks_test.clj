(ns automaton-build.tasks-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.tasks :as sut]))

(deftest create-bb-tasks
  (testing "Create bb tasks"
    (is (= {'foo {:doc "Test", :task '(execute)}
            'foo2 {:doc "Test2", :task '(execute)}}
           (sut/create-bb-tasks {"foo" {:cli-params-mode :none
                                        :doc "Test"
                                        :exec-task 'bar}
                                 "foo2" {:cli-params-mode :none
                                         :doc "Test2"
                                         :exec-task 'bar2}})))))
