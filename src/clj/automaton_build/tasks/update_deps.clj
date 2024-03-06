(ns automaton-build.tasks.update-deps
  (:require
   [automaton-build.app :as build-app]
   [automaton-build.os.npm :as build-npm]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Update the dependencies of the project"
  [_task-map
   {:keys [exclude-libs app-dir]
    :as _app}]
  (if (true? (and (build-app/update-app-deps app-dir exclude-libs)
                  (build-npm/npm-audit-fix app-dir)))
    build-exit-codes/ok
    build-exit-codes/cannot-execute))
