(ns automaton-build.code-helpers.formatter-test
  (:require
   [automaton-build.code-helpers.formatter :as sut]))

(comment
  ;; This test works only if some modifications are done in the bb.edn or
  ;; in the task. Note that it will remove the comment line
  (sut/format-file "bb.edn")
  ;
)
