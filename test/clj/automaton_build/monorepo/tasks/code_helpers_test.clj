(ns automaton-build.monorepo.tasks.code-helpers-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.monorepo.tasks.code-helpers :as sut]))

(def apps
  bafaaft/apps)

(comment
  (sut/gha apps {:force? true})

  (sut/outdated apps
                {})
;
  )
