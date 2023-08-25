(ns automaton-build.app.find-an-app-for-test
  "Utilitary function to find applications to test"
  (:require
   [automaton-build.core :as bc]))

(def apps
  (bc/create-apps))

(def one-app
  (first apps))

(def one-cust-app
  (last apps))
