(ns automaton-build.utils.keyword-test
  (:require
   [automaton-build.utils.keyword :as sut]
   [clojure.test                  :refer [deftest is testing]]))

(deftest trim-colon-test
  (testing "Is keyword colon trimmed"
    (is (= "test" (sut/trim-colon :test)))
    (is (= "test" (sut/trim-colon "test")))
    (is (= "test" (sut/trim-colon ":test")))
    (is (= "" (sut/trim-colon nil)))))
