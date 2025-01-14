(ns automaton-build.code.formatter-test
  (:require
   [automaton-build.code.formatter :as sut]
   [clojure.test                   :refer [deftest is]]))

(deftest formatter-setup-test
  (is (= :ok
         (-> (sut/formatter-setup)
             :status))
      "Is this environment well setup"))
