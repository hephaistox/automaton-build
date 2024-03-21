(ns automaton-build.tasks.visualize-deps
  (:require
   [automaton-build.doc.visualize-deps :as build-visualize-deps]
   [automaton-build.os.exit-codes      :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [output-file]}]
  (if (build-visualize-deps/visualize-deps output-file)
    build-exit-codes/ok
    build-exit-codes/catch-all))
