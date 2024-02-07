(ns automaton-build.code-helpers.code-stats-test
  (:require
   [automaton-build.code-helpers.code-stats :as sut]
   [clojure.test :refer [deftest is testing]]))

(deftest count-lines
  (testing "Count the line number in the files"
    (is (= 5
           (sut/count-lines ["file1" "file2"]
                            #(case %
                               "file1" "a\nb\n"
                               "file2" "a\nb\nc\n")
                            (constantly true)))))
  (testing "Empty project are ok"
    (is (= 0 (sut/count-lines [] (constantly true) (constantly true))))))
