(ns automaton-build.app.doc-test
  (:require
   [automaton-build.app.find-an-app-for-test :as find-an-app]
   [automaton-build.app.doc :as sut]))

(comment
  (sut/build-doc find-an-app/one-cust-app)
;
  )
