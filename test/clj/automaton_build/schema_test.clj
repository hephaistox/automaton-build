(ns automaton-build.schema-test
  (:require
   [automaton-build.schema :as sut]
   [clojure.test :refer [deftest is testing]]))

(deftest schema
  (testing "Valid schema"
    (is (sut/valid? [:tuple :string :int] ["hey" 12] "test1")))
  (testing "Invalid schema, throws an exception"
    (is (not (sut/valid? [:tuple :string :int] [12 12] "test2")))))
