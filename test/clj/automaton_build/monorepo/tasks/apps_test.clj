(ns automaton-build.monorepo.tasks.apps-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.monorepo.tasks.apps :as sut]))

(comment
  (sut/apps bafaaft/apps
            {})

;
  )
