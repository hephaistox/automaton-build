(ns automaton-build.os.commands-test
  (:require
   [automaton-build.os.commands :as sut]
   [clojure.test                :refer [deftest is testing]]))

(deftest execute-with-exit-code-test
  (testing "A simple command return"
    (is (= [[0 "foo\n"]] (sut/execute-with-exit-code ["echo" "foo"])))
    (is (= [[0 "foo\n"] [0 "bar\n"]]
           (sut/execute-with-exit-code ["echo" "foo"] ["echo" "bar"])))
    (is (= [0 -1]
           (mapv first (sut/execute-with-exit-code ["echo" "foo"] ["arg"]))))))

(deftest execute-and-trace-test
  (testing "failing command are detected"
    (is (not (sut/execute-and-trace ["this-command-does-not-exist"])))
    (is (not (sut/execute-and-trace ["pwd" {:out :string}]
                                    ["this-command-does-not-exist"]
                                    ["pwd" {:out :string}])))))

(deftest execute-get-string-test
  (testing "Get string actually return the strings"
    (is (= ["foo\n" "bar\n"]
           (sut/execute-get-string ["echo" "foo"] ["echo" "bar"])))))

(deftest first-cmd-failing-test
  (testing "Failing command is found"
    (is (= [3 "d"] (sut/first-cmd-failing [[0 "a"] [0 "b"] [0 "c"] [1 "d"]])))
    (is (= [1 "b"] (sut/first-cmd-failing [[0 "a"] [1 "b"] [0 "c"] [1 "d"]]))))
  (testing "Successful command is ok"
    (is (= [nil nil] (sut/first-cmd-failing [[0 ""] [0 ""] [0 ""] [0 ""]])))))

(deftest expand-cmd-test
  (testing "Expand command without options is ok"
    (is (= "foo bar" (sut/expand-cmd ["foo" "bar"]))))
  (testing "Expand command with options is ok"
    (is (= "foo bar" (sut/expand-cmd ["foo" "bar" {:bar :foo}])))))
