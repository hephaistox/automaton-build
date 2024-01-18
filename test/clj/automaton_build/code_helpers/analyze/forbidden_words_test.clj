(ns automaton-build.code-helpers.analyze.forbidden-words-test
  (:require
   [automaton-build.code-helpers.analyze.forbidden-words :as sut]
   [automaton-build.file-repo.clj-code :as build-clj-code]
   [clojure.test :refer [deftest is testing]]))

(def clj-repo
  (build-clj-code/->CljCodeFileRepo {"foo.clj" ["  automaton-foobar"]
                                     "foo2.clj" ["  automaton_foobar"]}))

(deftest execute-report-test
  (let [clj-repo (build-clj-code/->CljCodeFileRepo
                  {"foo.clj" ["  automaton-foobar"]
                   "foo2.clj" ["  automaton_foobar"]})]
    (testing "Comment report is returning expected lines"
      (is (= [["foo.clj" ["automaton-foobar"] "automaton-foobar"]
              ["foo2.clj" ["automaton_foobar"] "automaton_foobar"]]
             (sut/forbidden-words-matches (sut/coll-to-alternate-in-regexp
                                           [#"automaton[-_]foobar"
                                            #"automaton[-_]foobar"])
                                          clj-repo))))))

(deftest coll-to-alternate-in-regexp-test
  (testing "One word is transformed"
    (is (= "(automaton[-_]foobar|automaton[-_]foobar)"
           (str (sut/coll-to-alternate-in-regexp [#"automaton[-_]foobar"
                                                  #"automaton[-_]foobar"]))))))
