#_{:heph-ignore {:comments true}}
(ns automaton-build.code-helpers.analyze.comments-test
  (:require
   [automaton-build.code-helpers.analyze.comments :as sut]
   [automaton-build.file-repo.clj-code            :as build-clj-code]
   [automaton-build.file-repo.text                :as build-filerepo-text]
   [clojure.test                                  :refer [deftest is testing]]))

(deftest comment-pattern-test
  (testing
    "T O D O s are found (space intentionally left so rg won't find the word)"
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format "  ;;%s  " sut/T))))
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format ";;%s " sut/T))))
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format "  ;;       %s "
                                                        sut/T))))
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format ";;%s This is a to do"
                                                        sut/T)))))
  (testing "Check other words"
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format "  ;;%s  " sut/N))))
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format "  ;;%s " sut/D))))
    (is (some? (build-filerepo-text/search-line sut/comments-pattern
                                                (format "  ;;%s " sut/F))))))

(deftest execute-report-test
  (let [clj-repo (build-clj-code/->CljCodeFileRepo
                  {"foo.clj" [(format ";;%s foo is bar" sut/T)]})]
    (testing "Comment report is returning expected lines"
      (is (= [[" foo is bar" "foo.clj" (format ";;%s foo is bar" sut/T)]]
             (sut/comment-matches clj-repo))))))
