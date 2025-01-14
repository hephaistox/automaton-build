(ns automaton-build.tasks.lint-test
  (:require
   [automaton-build.tasks.lint :as sut]
   [clojure.test               :refer [deftest is]]))

(deftest lint-test
  (comment
    (sut/lint {:normalln println
               :errorln println}
              true
              ""
              ["src"])
    (sut/lint-one-line-headers true "" ["src"])
    ;
  ))
