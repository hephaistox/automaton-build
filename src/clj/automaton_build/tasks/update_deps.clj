(ns automaton-build.tasks.update-deps
  (:require
   [automaton-build.os.npm :as build-npm]
   [automaton-build.code-helpers.update-deps :as build-code-update-deps]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Update the dependencies of the project"
  [_task-map
   {:keys [exclude-libs app-dir exclude-dirs]
    :as _app}]
  (if (true? (and (build-code-update-deps/update-app-deps app-dir
                                                          exclude-libs
                                                          exclude-dirs)
                  (build-npm/npm-audit-fix app-dir)))
    build-exit-codes/ok
    build-exit-codes/cannot-execute))
