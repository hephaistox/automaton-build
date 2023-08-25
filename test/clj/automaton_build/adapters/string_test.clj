(ns automaton-build.adapters.string-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.string :as sut]))

(deftest remove-last-character
  (testing "Remove a character"
    (is (= "This"
           (sut/remove-last-character "Thiss")))
    (is (= "This"
           (sut/remove-last-character "Thisù"))))
  (testing "One character is removed"
    (is (= ""
           (sut/remove-last-character "T")))
    (is (= ""
           (sut/remove-last-character "ù"))))
  (testing "Empty string is ok"
    (is (= ""
           (sut/remove-last-character "")))
    (is (= ""
           (sut/remove-last-character nil)))))

(deftest limit-length-test
  (testing "Ellipsis is limited to "
    (is (= "f..."
           (sut/limit-length "foobar" 4)))
    (is (= "fo..."
           (sut/limit-length "foobar" 5))))
  (testing "Ellipsis doesn't change short enough string"
    (is (= "foobar"
           (sut/limit-length "foobar" 100)))
    (is (= "foobar"
           (sut/limit-length "foobar" 6))))
  (testing "Prefix and suffix are taken into account"
    (is (= "aafoobarbb"
           (sut/limit-length "foobar" 10 "aa" "bb" (constantly nil))))
    (is (= "aafoobarbb"
           (sut/limit-length "foobar" 1000 "aa" "bb" (constantly nil))))
    (is (= "aafo...bb"
           (sut/limit-length "foobar" 9 "aa" "bb" (constantly nil))))))
