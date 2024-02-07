(ns automaton-build.tasks.tasks
  (:require
   [automaton-build.tasks.registry :as build-task-registry]
   [automaton-build.cicd.cfg-mgt]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "List all tasks"
  [_task-map
   {:keys [task-registry]
    :as app-data}]
  (if (nil? (build-task-registry/print-tasks task-registry app-data))
    build-exit-codes/ok
    build-exit-codes/catch-all))
