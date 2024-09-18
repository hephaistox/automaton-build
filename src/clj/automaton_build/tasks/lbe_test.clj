(ns automaton-build.tasks.lbe-test
  (:require
   [automaton-build.os.commands   :as build-cmds]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Local backend tests
  All that tests should be runnable on github action
  `rlwrap` is not on the container image, so `clojure` should be used instead of `clj`"
  [_task-map {:keys [test-aliases]}]
  (if-not (build-cmds/execute-and-trace ["clojure" (apply str "-M" test-aliases)])
    build-exit-codes/catch-all
    build-exit-codes/ok))
