(ns automaton-build.app.test-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.app.test :as sut]))

(comment
  (sut/test-linters-wip bafaaft/one-app)

  (sut/test-linters-clean-code bafaaft/one-app)

  (sut/test-backend bafaaft/one-app)

  (sut/test-frontend bafaaft/one-app)
;
  )
