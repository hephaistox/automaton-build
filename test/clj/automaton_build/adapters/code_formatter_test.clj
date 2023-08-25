(ns automaton-build.adapters.code-formatter-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.code-formatter :as sut]))

(deftest format-content-test
  (testing "Simple test"
    (is (= "{:a :b}"
           (sut/format-content {:a    :b})))
    (is (= "(do {:a {:b :c}} 1)"
           (sut/format-content '(do {:a {:b :c}} 1))))))

(comment
  ;; This test works only if some modifications are done in the bb.edn or in the task
  ;; Note that it will remove the comment line
  (sut/format-file "bb.edn")
;
  )
