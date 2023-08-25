(ns automaton-build.adapters.schema-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.schema :as sut]))

(deftest schema
  (testing "Valid schema"
    (is (sut/schema-valid [:tuple :string :int]
                          ["hey" 12])))
  (testing "Invalid schema, throws an exception"
    (is (not (sut/schema-valid [:tuple :string :int]
                               [12 12]
                               )))))

(deftest schema-valid-or-throw
  (testing "Valid schema"
    (is (sut/schema-valid-or-throw [:tuple :string :int]
                                   ["hey" 12]
                                   "Tuple shouldn't raise an exception")))
  (testing "Invalid schema, throws an exception"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Tuple raises an exception"
                          (sut/schema-valid-or-throw [:tuple :string :int]
                                                     [12 12]
                                                     "Tuple raises an exception")))))
