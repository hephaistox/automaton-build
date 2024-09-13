#_{:heph-ignore {:css true}}
(ns automaton-build.code-helpers.analyze.css-test
  (:require
   [automaton-build.code-helpers.analyze.css :as sut]
   [automaton-build.file-repo.text           :as build-filerepo-text]
   [clojure.test                             :refer [deftest is testing]]))

(deftest css-pattern-test
  (testing "Detect class string"
    (is (not (nil? (build-filerepo-text/search-line sut/css-pattern
                                                    "This :class \"should be discovered"))))
    (is (not (nil? (build-filerepo-text/search-line sut/css-pattern
                                                    "This :class     \"should be discovered"))))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :class should not be discovered"))))
  (testing "Class vectors are expected whatever the form"
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :class (apply vec should be discovered")))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :class (apply \nvec should be discovered")))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :class (vec should be discovered")))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :class\n(\nvector\n should be discovered"))))
  (testing "Accept class litteral vectors"
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :class [... should be accepted"))))
  (testing "Dectect class on html element"
    (is (not (nil? (build-filerepo-text/search-line sut/css-pattern
                                                    "This :a#id should be discovered")))))
  (testing "Dectect class id on html element"
    (is (not (nil? (build-filerepo-text/search-line sut/css-pattern
                                                    "This :a.foo should be discovered"))))
    (is (not (nil? (build-filerepo-text/search-line sut/css-pattern
                                                    "This :a#foo should be discovered"))))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This :annn.foo should not be discovered")))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This .foo should not be discovered")))
    (is (nil? (build-filerepo-text/search-line sut/css-pattern
                                               "This #foo should not be discovered")))))
