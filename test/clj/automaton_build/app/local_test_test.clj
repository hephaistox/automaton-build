(ns automaton-build.app.local-test-test
  (:require
   [automaton-build.app.local-test :as sut]
   [automaton-build.app.find-an-app-for-test :as bafaaft]))

(comment
  (sut/ltest bafaaft/one-cust-app)
  ;
  )
