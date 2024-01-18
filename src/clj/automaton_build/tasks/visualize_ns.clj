(ns automaton-build.tasks.visualize-ns
  (:require
   [automaton-build.doc.visualize-ns :as build-visualize-ns]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [output-file]}]
  (if-not (build-visualize-ns/visualize-ns output-file)
    build-exit-codes/catch-all
    build-exit-codes/ok))
