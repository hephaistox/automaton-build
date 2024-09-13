(ns automaton-build.containers.local-engine-test
  (:require
   [automaton-build.containers.local-engine :as sut]
   [clojure.test                            :refer [deftest is testing]]))

(deftest container-installed?-test
  (testing "Detect non existing command"
    (is (not (sut/container-installed?* "non-existing-command"))))
  (testing "Detect existing command" (is (sut/container-installed?* "echo"))))
