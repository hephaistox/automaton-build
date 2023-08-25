(ns automaton-build.adapters.frontend-compiler-test
  (:require
   [clojure.test :refer [deftest is testing]]

   [automaton-build.adapters.cicd :as cicd]
   [automaton-build.adapters.frontend-compiler :as sut]))

(when-not (cicd/is-cicd?)
  (deftest npx-installed?
    (testing "Is able to detect non working npx command"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"npx is not working"
                            (sut/npx-installed? "." "non-npx-command"))))))

(when-not (cicd/is-cicd?)
  (deftest shadow-installed?
    (testing "Is able to detect non working shadow command"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"shadow is not working"
                            (sut/shadow-installed? "." "non-shadow-command" "npx"))))))

(comment
  (sut/compile-target "npx"
                      "karma-test"
                      "automaton")

  (sut/test-fe "automaton")

  (sut/create-size-optimization-report "template_app")

  (sut/watch-modifications "automaton")
;
  )
