(ns automaton-build.log-test
  (:require
   [automaton-build.log :as sut]
   [clojure.test        :refer [deftest is testing]]))

(deftest log-level-to-ix-test
  (testing "Compare accepted log levels" (is (sut/compare-log-levels :trace :debug)))
  (testing "Compare non accepted log levels" (is (not (sut/compare-log-levels :debug :trace))))
  (testing "Less than two arguments is ok" (sut/compare-log-levels :debug) (sut/compare-log-levels))
  (testing "Less than two arguments is ok" (sut/compare-log-levels :trace :warning :fatal)))

(deftest trace-test
  (with-redefs [sut/min-level (atom :info)]
    (testing "If log level set is too high, message is skipped"
      (is (= "" (with-out-str (sut/trace "message"))))))
  (with-redefs [sut/min-level (atom :trace)]
    (testing "If log level is low enough, message is printed"
      (is (sut/compare-log-levels :trace :info))
      (is (sut/compare-log-levels @sut/min-level :trace))
      (comment ;; That test can't work during `bb ltest` as logging is
               ;; skipped with `hephaistox-in-test` java property
        (is (re-find #"\s*--> message" (with-out-str (sut/trace "message"))))))))
