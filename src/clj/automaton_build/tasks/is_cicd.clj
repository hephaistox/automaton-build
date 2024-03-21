(ns automaton-build.tasks.is-cicd
  (:require
   [automaton-build.cicd.server   :as build-cicd-server]
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Test if the current system is cicd or not"
  [_task-map app-data]
  (let [{:keys [force]} app-data]
    (if-not (or (build-cicd-server/is-cicd?) force)
      (do
        (build-log/fatal
         "This task is meant for CI, use `bb wf-2` instead (or -f to force it to test it locally)")
        build-exit-codes/catch-all)
      build-exit-codes/ok)))
