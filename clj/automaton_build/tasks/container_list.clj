(ns automaton-build.tasks.container-list
  (:require
   [automaton-build.containers.local-engine :as build-local-engine]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.terminal-msg :as build-terminal-msg]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "List all available containers"
  [_task-map _app]
  (let [[exit-code res] (-> (build-local-engine/container-image-list)
                            first)]
    (if (zero? exit-code)
      (do (build-terminal-msg/println-msg res) build-exit-codes/ok)
      (do (build-log/warn "Container list failed, check if docker is turned on")
          exit-code))))
