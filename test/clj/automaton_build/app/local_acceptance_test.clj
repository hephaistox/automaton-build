(ns automaton-build.app.local-acceptance-test
  (:require
   [automaton-build.app.local-acceptance :as sut]
   [automaton-build.app.find-an-app-for-test :as bafaaft]))

(comment
  (sut/la bafaaft/one-cust-app
          "manual test"
          true)

  (sut/la bafaaft/one-doc-app
          "manual test"
          true)
;
  )
