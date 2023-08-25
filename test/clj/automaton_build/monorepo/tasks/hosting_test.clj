(ns automaton-build.monorepo.tasks.hosting-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.monorepo.tasks.hosting :as sut]))

(def apps
  bafaaft/apps)

(comment
  (sut/pconnect {}
                {})
;
  )
