(ns automaton-build.tasks.container-clear
  (:require
   [automaton-build.containers.local-engine :as build-local-engine]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map _app]
  (build-log/info "Clean the containers")
  (if (nil? (build-local-engine/container-clean))
    build-exit-codes/ok
    build-exit-codes/catch-all))
