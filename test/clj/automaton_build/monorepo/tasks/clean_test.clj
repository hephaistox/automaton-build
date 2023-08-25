(ns automaton-build.monorepo.tasks.clean-test
  (:require
   [automaton-build.monorepo.tasks.clean :as sut]
   [automaton-build.app.find-an-app-for-test :as bafaaft]))

(comment
  (sut/clean bafaaft/apps
             {})
;
  )
