(ns automaton-build.code-helpers.build-config-test
  (:require
   [automaton-build.app.build-config :as sut]
   [clojure.test :refer [deftest is testing]]))

(deftest read-build-config-test
  (testing "Read the current build config"
    (is (map? (sut/read-build-config ""))))
  (testing "Read the current build config"
    (is (nil? (sut/read-build-config "non-existing-dir")))))
