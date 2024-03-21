(ns automaton-build.tasks.lfe-test
  (:require
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.log                            :as build-log]
   [automaton-build.os.exit-codes                  :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Local frontend tests
  All that tests should be runnable on github action
  `rlwrap` is not on the container image, so `clojure` should be used instead of `clj`"
  [_task-map {:keys [app-dir]}]
  (if-not (build-frontend-compiler/fe-test app-dir)
    (do (build-log/fatal "Tests have failed") build-exit-codes/catch-all)
    build-exit-codes/ok))
