(ns automaton-build.tasks.update-deps
  (:require
   [automaton-build.code-helpers.update-deps :as build-code-update-deps]
   [automaton-build.os.exit-codes            :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Update the dependencies of the project"
  [_task-map
   {:keys [exclude-libs app-dir]
    :as _app}]
  (if (true? (build-code-update-deps/update-app-deps app-dir exclude-libs))
    build-exit-codes/ok
    build-exit-codes/cannot-execute))
