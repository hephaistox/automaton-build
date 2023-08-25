(ns automaton-build.app-agnostic.code-stats-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.app-agnostic.code-stats :as sut]))

(deftest count-lines
  (testing "Count the line number in the files"
    (is (= 5
           (sut/count-lines ["file1" "file2"]
                            #(case %
                               "file1" "a\nb\n"
                               "file2" "a\nb\nc\n")
                            (constantly true))))))
