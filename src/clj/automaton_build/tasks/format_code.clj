(ns automaton-build.tasks.format-code
  (:require
   [automaton-build.code-helpers.formatter :as build-code-formatter]
   [automaton-build.os.exit-codes          :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Format all code files"
  [_task-map {:keys [include-files app-dir]}]
  (cond
    (= :fail (build-code-formatter/format-clj app-dir))
    build-exit-codes/catch-all
    (not (build-code-formatter/files-formatted include-files))
    build-exit-codes/catch-all
    :else build-exit-codes/ok))
