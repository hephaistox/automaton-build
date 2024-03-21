(ns automaton-build.utils.uuid-gen-test
  (:require
   [automaton-build.utils.uuid-gen :as sut]
   [clojure.test                   :refer [deftest is testing]]))

(deftest unguessable
  (testing "check that generates proper uuid"
    (is (every? uuid? (repeatedly 10 #(sut/time-based-uuid))))))
