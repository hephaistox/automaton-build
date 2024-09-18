(ns automaton-build.tasks.clean-state
  (:require
   [automaton-build.cicd.cfg-mgt  :as build-cfg-mgt]
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn clean-state? [app-dir] (not (build-cfg-mgt/git-changes? app-dir)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [app-dir]}]
  (if (clean-state? app-dir)
    build-exit-codes/ok
    (do (build-log/warn "State is not clean") build-exit-codes/catch-all)))
