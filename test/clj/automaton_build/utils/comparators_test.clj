(ns automaton-build.utils.comparators-test
  (:require
   [automaton-build.utils.comparators :as sut]
   [clojure.test                      :refer [deftest is testing]]))

(deftest comparator-kw-symbol-test
  (testing "Keywords first" (is (sut/comparator-kw-symbol :a 'foo)))
  (testing "Keywords alphabetically sorted"
    (is (sut/comparator-kw-symbol :a :z))
    (is (not (sut/comparator-kw-symbol :z :a))))
  (testing "Symbols alphabetically sorted"
    (is (sut/comparator-kw-symbol 'a 'z))
    (is (not (sut/comparator-kw-symbol 'z 'a)))))
