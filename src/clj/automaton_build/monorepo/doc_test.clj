(ns automaton-build.monorepo.doc-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.monorepo.doc :as sut]))

(comment
  (sut/build-doc bafaaft/apps)
 ;
  )
