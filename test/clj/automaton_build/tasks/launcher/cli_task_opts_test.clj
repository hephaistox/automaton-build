(ns automaton-build.tasks.launcher.cli-task-opts-test
  (:require
   [automaton-build.tasks.launcher.cli-task-opts :as sut]
   [clojure.test :refer [deftest is testing]]))

(deftest cli-opts-spec-test
  (testing "Find at least the 3 common options, even if tasks has no option"
    (is (<= 3 (count (sut/cli-opts-spec nil)))))
  (testing "Are the options found, both tasks and tasks agnostic"
    (is (<= 5 (count (sut/cli-opts-spec [:force :tag]))))))

(def stub
  {:options {:log :info
             :force true}
   :arguments []
   :summary "fake summary"
   :errors nil})

(deftest mandatory-option-test
  (testing "Mandatory option is required"
    (is (sut/mandatory-option-present? stub [:force])))
  (testing "Not mandatory option is not required"
    (is (re-find #"mandatory and missing"
                 (with-out-str (sut/mandatory-option-present? stub
                                                              [:message]))))))

(deftest cli-opts-test
  (testing "Boolean Force argument is found"
    (is (get-in (sut/cli-opts [:force] ["-f"]) [:options :force])))
  (testing "Non required argument is found"
    (is (some? (:errors (sut/cli-opts [] ["-f"]))))))
