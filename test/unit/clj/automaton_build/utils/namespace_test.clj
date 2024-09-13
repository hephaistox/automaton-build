(ns automaton-build.utils.namespace-test
  (:require
   [automaton-build.utils.namespace :as sut]
   [clojure.test                    :refer [deftest is testing]]))

(deftest update-last-test
  (testing "Add clj to last element" (is (= ["aze" "foo.clj"] (sut/update-last "aze" "foo"))))
  (testing "Add clj to last element" (is (nil? (sut/update-last)))))

(deftest ns-to-file-test
  (testing "Is ok"
    (is (= "automaton_build/utils/namespace_test.clj"
           (sut/ns-to-file 'automaton-build.utils.namespace-test)))))

(defn foo [args] ["foo" args])

(defn foo-ex [args] (throw (ex-info "on purpose" {:args args})))

(deftest symbol-to-fn-call-test
  (testing "Existing functions are called"
    (is (= ["foo" "bar"] (sut/symbol-to-fn-call 'automaton-build.utils.namespace-test/foo "bar"))))
  (testing "Non existing functions are ok"
    (is (nil? (sut/symbol-to-fn-call 'automaton-build.utils.namespace-test/foo2 "bar"))))
  (testing "Non existing namespaces are ok"
    (is (nil? (sut/symbol-to-fn-call 'automaton-build.utils.namespace-test2/foo "bar"))))
  (testing "Exceptions during function call returns with nil"
    (is (nil? (sut/symbol-to-fn-call 'automaton-build.utils.namespace-test/foo-ex
                                     "on purpose ex")))))

(deftest namespaced-keyword-test
  (testing "Test from symbols" (is (= 'foo/bar (sut/namespaced-keyword 'foo 'bar))))
  (testing "Test from kw" (is (= 'foo/bar (sut/namespaced-keyword :foo :bar))))
  (testing "Test from string" (is (= 'foo/bar (sut/namespaced-keyword "foo" "bar"))))
  (testing "Resist to nul value "
    (is (= 'bar (sut/namespaced-keyword nil :bar)))
    (is (= 'foo (sut/namespaced-keyword :foo nil)))))
