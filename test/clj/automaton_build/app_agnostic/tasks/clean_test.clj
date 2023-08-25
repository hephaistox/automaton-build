(ns automaton-build.app-agnostic.tasks.clean-test
  (:require
   [automaton-build.app-agnostic.tasks.clean :as sut]))

(comment
  ; !!! Use with parcimony it will clean the whole repo
  (sut/clean-hard {} {})
  ;
  )
