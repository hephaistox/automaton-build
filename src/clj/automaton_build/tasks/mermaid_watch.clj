(ns automaton-build.tasks.mermaid-watch
  (:require
   [automaton-build.doc.mermaid   :as build-mermaid]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [app-dir]}]
  (if (nil? (build-mermaid/watch app-dir)) build-exit-codes/ok build-exit-codes/catch-all))
