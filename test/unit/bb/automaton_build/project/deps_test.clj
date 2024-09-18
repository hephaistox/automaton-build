(ns automaton-build.project.deps-test
  (:require
   [automaton-build.project.deps :as sut]
   [clojure.test                 :refer [deftest is]]))

(deftest deps-edn-test (is (:edn (sut/deps-edn "")) "Current repl deps edn should be readable"))

(deftest get-src-test
  (is (= ["a/src" "b/src" "c/src"]
         (sut/get-src {:paths ["a/src"]
                       :aliases {:a1 {:extra-paths ["b/src"]}
                                 :a2 {:extra-paths ["c/src"]}}}))
      "src from root and extra-paths from aliases are all returned."))
