(ns automaton-build.app-agnostic.tasks-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [automaton-core.adapters.schema :as schema]
   [automaton-build.tasks :as bt]
   [automaton-build.app-agnostic.tasks :as sut]))

(deftest tasks-test
  (testing "Tasks are compliant with their schema"
    (is (schema/schema-valid-or-throw bt/task-schema
                                      sut/tasks
                                      "app-agnostic tasks are not compliant to the schema"))))
