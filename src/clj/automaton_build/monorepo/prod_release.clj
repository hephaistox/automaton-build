(ns automaton-build.monorepo.prod-release
  (:require
   [automaton-build.adapters.log :as log]
   [automaton-build.app.test :as app-test]))

(defn prelease
  "Tasks publishing a customer application from the monorepo
  Params:
  * `apps` applications"
  [apps]
  (log/warn "This is not implemented yet")
  (doseq [app apps]
    (app-test/test-linters-clean-code app)))
