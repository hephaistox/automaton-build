(ns automaton-build.tasks.docs-test
  (:require
   [automaton-build.tasks.docs :as sut]))

(comment
  (sut/run-monorepo "**{.png,.gif,.jpg,.svg}" "gh-pages-dont-change")
  ;
)
