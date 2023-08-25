(ns automaton-build.app.local-test
  "Local testing"
  (:require
   [automaton-build.adapters.log :as log]
   [automaton-build.app.test :as app-test]))

(defn ltest
  "Test locally an application
  Params:
  * `app` application"
  [app]
  (log/info "Test app `" (:app-name app) "`")
  (app-test/test-linters-wip app)
  (app-test/test-frontend app))
