(ns automaton-build.app.running-test
  (:require
   [automaton-build.app.find-an-app-for-test :as bafaaft]
   [automaton-build.app.running :as sut]))

(comment
  (sut/run-prepl bafaaft/one-cust-app
                 "everything")

  (sut/run-prod-be bafaaft/one-cust-app)

  (sut/watch-cljs bafaaft/one-cust-app)
  ;
  )
