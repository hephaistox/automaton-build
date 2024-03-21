(ns automaton-build.utils.seq-test
  (:require
   [automaton-build.utils.seq :as sut]
   [clojure.test              :refer [deftest is testing]]))

(deftest contains?-test
  (testing "Sequence contains value"
    (is (true? (sut/contains? [1 2 3 4 5] 5)))
    (is (nil? (sut/contains? [1 2 3 4 5] 6)))
    (is (true? (sut/contains? ["ala" "ma" "kota"] "kota")))
    (is (nil? (sut/contains? ["ala" "ma" "kota"] "m"))))
  (testing "Sequence contains multiple values"
    (is (true? (sut/contains? [1 2 3 4 5 6] 1 2 3)))
    (is (nil? (sut/contains? [1 2 3] 1 2 4)))))
