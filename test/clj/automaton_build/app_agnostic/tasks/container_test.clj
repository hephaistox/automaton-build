(ns automaton-build.app-agnostic.tasks.container-test
  (:require
   [automaton-build.app-agnostic.tasks.container :as sut]))

(comment
  (sut/container-image-list {} {})
  (sut/container-clean {} {})
;
  )
