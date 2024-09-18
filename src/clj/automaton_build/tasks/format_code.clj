(ns automaton-build.tasks.format-code
  (:require
   [automaton-build.code-helpers.formatter :as build-code-formatter]
   [automaton-build.os.exit-codes          :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Format all code files"
  [_task-map {:keys [app-dir]}]
  (if (= :fail (build-code-formatter/format-clj app-dir))
    build-exit-codes/catch-all
    build-exit-codes/ok))
