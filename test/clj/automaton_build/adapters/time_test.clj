(ns automaton-build.adapters.time-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.time :as sut]))

(deftest now-str
  (testing "Check date is generated"
    (let [date-str (sut/now-str)]
      (is (string? date-str))
      (is (> (count date-str)
             20)))))
