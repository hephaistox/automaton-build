(ns automaton-build.echo.actions-test
  (:require
   [automaton-build.echo.actions :as sut]
   [clojure.test                 :refer [deftest is]]))

(deftest normalln-test
  (is (= "az>a b\n[39m" (with-out-str (sut/normalln ["az"] "a" "b"))) "Simple use case")
  (is (= ">a b\n[39m" (with-out-str (sut/normalln [] "a" "b"))) "No prefix")
  (is (= "a-b-c>a b\n[39m" (with-out-str (sut/normalln ["a" "b" "c"] "a" "b")))
      "Multiple prefixs"))
