(ns automaton-build.monorepo.tasks-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.schema :as schema]
   [automaton-build.monorepo.tasks :as sut]))

(deftest tasks
  (testing "Check tasks definition correctness"
    (is (schema/schema-valid-or-throw [:vector [:tuple :string [:map {:closed true}
                                                               [:doc {:optional true} :string]
                                                               [:create-deps-at-startup? {:optional true} :boolean]
                                                               [:cli-params-mode :keyword]
                                                               [:exec-task fn?]]]]
                                      sut/tasks
                                      (format "Tasks definition in %s is badly defined"
                                              (name 'sut))))))
