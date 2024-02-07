(ns automaton-build.tasks.mermaid
  (:require
   [automaton-build.doc.mermaid :as build-mermaid]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [mermaid-dir]}]
  (if (build-mermaid/build-all-files mermaid-dir)
    build-exit-codes/ok
    build-exit-codes/catch-all))
