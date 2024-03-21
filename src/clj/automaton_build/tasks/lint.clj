(ns automaton-build.tasks.lint
  (:require
   [automaton-build.app-data           :as build-app-data]
   [automaton-build.cicd.cfg-mgt]
   [automaton-build.code-helpers.lint  :as build-lint]
   [automaton-build.file-repo.clj-code :as build-clj-code]
   [automaton-build.log                :as build-log]
   [automaton-build.os.exit-codes      :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Linter"
  [_task-map app-data]
  (let [repo-path-files (build-app-data/project-paths-files-content app-data)
        filenames (build-clj-code/filenames repo-path-files)]
    (if-not (build-lint/lint-files false filenames)
      (do (build-log/fatal "Linters have failed") build-exit-codes/catch-all)
      build-exit-codes/ok)))
