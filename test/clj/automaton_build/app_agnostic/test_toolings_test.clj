(ns automaton-build.app-agnostic.test-toolings-test
  (:require
   [clojure.test :refer [deftest testing is]]

   [automaton-build.app-agnostic.test-toolings :as sut]))

(comment
  (sut/unit-test ".")
  ;
  )

(def code-files-repo-stub
  {"test-toolings-test.clj" ["(ns automaton-buildtest-toolings-test"
                             "(:require"
                             "[clojure.test :refer [deftest testing is]]"
                             ""
                             "[automaton-build.test-toolings :as btt]))"
                             "[automaton-build.core :as bc]))"
                             ""]
   "foo.clj" ["(ns foo"
              "(:require"
              "[clojure.set :refer [union]]"
              "[automaton-build.test-toolings :as bt]))"
              "[automaton-build.test-test :as btt]))"
              ""
              "This is ;;TODO \n;;  \n  DONE ;; NOTE ;; FIXME"
              "[automaton-build.core :as bc]))"
              ""]
   "foo-test.clj" ["(ns foo-test"
                   "(:require"
                   "[clojure.test :refer [deftest is testing]]"
                   ""
                   "[foo :as sut]))"
                   ""]})

(deftest search-line
  (testing "Search line"
    (is (nil? (sut/search-line #"ff"
                               "This text is not")))
    (is (= "ff"
           (sut/search-line #"ff"
                            "This text is containing ff !")))))

(deftest comment-pattern-test
  (testing "TODOs are found"
    (is (sut/search-line sut/comment-pattern "  ;;TODO  "))
    (is (sut/search-line sut/comment-pattern ";;TODO  "))
    (is (sut/search-line sut/comment-pattern "  ;;       TODO  "))
    (is (sut/search-line sut/comment-pattern ";;TODO This is a to do")))
  (testing "NOTE DONE and FIXME"
    (is (sut/search-line sut/comment-pattern "  ;;NOTE  "))
    (is (sut/search-line sut/comment-pattern "  ;;DONE  "))
    (is (sut/search-line sut/comment-pattern "  ;;FIXME "))))

(deftest assert-comments
  (testing "TODOs, DONE, NOTE and FIXME are found"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Found forbidden comments in the code"
                          (sut/assert-comments code-files-repo-stub)))))

(deftest css-pattern
  (testing "Detect class string"
    (is (not (nil?
              (sut/search-line sut/css-pattern
                               "This :class \"should be discovered"))))
    (is (not (nil?
              (sut/search-line sut/css-pattern
                               "This :class     \"should be discovered"))))
    (is (nil? (sut/search-line sut/css-pattern
                               "This :class should not be discovered"))))
  (testing "Class vectors are expected whatever the form"
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This :class (apply vec should be discovered")))
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This :class (apply \nvec should be discovered")))
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This :class (vec should be discovered")))
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This :class\n(\nvector\n should be discovered"))))
  (testing "Accept class litteral vectors"
    (is (nil? (sut/search-line sut/css-pattern
                               "This :class [... should be accepted"))))
  (testing "Dectect class on html element"
    (is (not (nil?
              (sut/search-line sut/css-pattern
                               "This :a#id should be discovered")))))
  (testing "Dectect class id on html element"
    (is (not (nil?
              (sut/search-line sut/css-pattern
                               "This :a.foo should be discovered"))))
    (is (not (nil?
              (sut/search-line sut/css-pattern
                               "This :a#foo should be discovered"))))
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This :annn.foo should not be discovered")))
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This .foo should not be discovered")))
    (is (nil?
         (sut/search-line sut/css-pattern
                          "This #foo should not be discovered")))))

(deftest generate-lint-all-cmds
  (testing "Check all directories are included in the linter for all apps"
    (is (= [[["clj-kondo"
              "--lint"
              "test-toolings-test.clj"
              "--lint"
              "foo.clj"
              "--lint"
              "foo-test.clj"]]]
           (sut/generate-lint-all-cmds code-files-repo-stub)))))

(deftest search-aliases-in-line-test
  (testing "Variants without aliases"
    (is (nil?
         (sut/search-line sut/alias-pattern "[]"))))
  (testing "Variants with aliases"
    (is (= ["[ff :as tt]" "ff" "tt" nil]
           (sut/search-line sut/alias-pattern "[ff :as tt]")))
    (is (= ["[Ff :as tt]" "Ff" "tt" nil]
           (sut/search-line sut/alias-pattern "[Ff :as tt]")))
    (is (= ["[f0f :as tt]" "f0f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f0f :as tt]"))))
  (testing "Variants with complex names aliases"
    (is (= ["[f*0 :as tt]" "f*0" "tt" nil]
           (sut/search-line sut/alias-pattern "[f*0 :as tt]")))
    (is (= ["[f+f :as tt]" "f+f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f+f :as tt]")))
    (is (= ["[f!f :as tt]" "f!f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f!f :as tt]")))
    (is (= ["[f-f :as tt]" "f-f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f-f :as tt]")))
    (is (= ["[f_f :as tt]" "f_f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f_f :as tt]")))
    (is (= ["[f'f :as tt]" "f'f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f'f :as tt]")))
    (is (= ["[f?f :as tt]" "f?f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f?f :as tt]")))
    (is (= ["[f<f :as tt]" "f<f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f<f :as tt]")))
    (is (= ["[f>f :as tt]" "f>f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f>f :as tt]")))
    (is (= ["[f=f :as tt]" "f=f" "tt" nil]
           (sut/search-line sut/alias-pattern "[f=f :as tt]")))
    (is (= ["[f=f :refer [tt uu]]" "f=f" nil ":refer"]
           (sut/search-line sut/alias-pattern "[f=f :refer [tt uu]]")))
    (is (= ["[fF9*+!-'?<>=f :as tt]" "fF9*+!-'?<>=f" "tt" nil]
           (sut/search-line sut/alias-pattern "[fF9*+!-'?<>=f :as tt]")))
    (is (= ["[f-f :as fF9*+!-'?<>=f]" "f-f" "fF9*+!-'?<>=f" nil]
           (sut/search-line sut/alias-pattern "[f-f :as fF9*+!-'?<>=f]"))))
  (testing "Spaces are caught"
    (is (= ["  [  ff   :as   tt ]  " "ff" "tt" nil]
           (sut/search-line sut/alias-pattern "  [  ff   :as   tt ]  ")))
    (is (= ["   [automaton-build.adapters.log :as log]" "automaton-build.adapters.log" "log" nil]
           (sut/search-line sut/alias-pattern "   [automaton-build.adapters.log :as log]")))))

(deftest namespace-report-test
  (testing "Hey"
    (is (= {"automaton-build.test-toolings" {"btt" ["test-toolings-test.clj"], "bt" ["foo.clj"]},
            "automaton-build.core" {"bc" ["test-toolings-test.clj" "foo.clj"]},
            "automaton-build.test-test" {"btt" ["foo.clj"]}}
           (sut/namespace-report code-files-repo-stub)))))

(deftest namespace-has-one-alias-test
  (testing "The multiple aliases are detected"
    (is (= {"automaton-build.test-toolings" {"btt" ["test-toolings-test.clj"]
                                             "bt" ["foo.clj"]}}
           (sut/namespace-has-one-alias code-files-repo-stub)))))

(deftest alias-report-test
  (testing "Hey"
    (is (= {"btt" {"automaton-build.test-toolings" ["test-toolings-test.clj"]
                   "automaton-build.test-test" ["foo.clj"]},
            "bc" {"automaton-build.core" ["test-toolings-test.clj" "foo.clj"]},
            "bt" {"automaton-build.test-toolings" ["foo.clj"]}}
           (sut/alias-report code-files-repo-stub)))))

(deftest alias-has-one-namespace-test
  (testing "The multiple aliases are detected"
    (is (= {"btt" {"automaton-build.test-toolings" ["test-toolings-test.clj"]
                   "automaton-build.test-test" ["foo.clj"]}}
           (sut/alias-has-one-namespace code-files-repo-stub)))))
