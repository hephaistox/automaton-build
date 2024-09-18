(ns automaton-build.tasks.clean
  (:require
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files      :as build-files]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Clean cache files for compilers to start from scratch"
  [_task-map {:keys [dirs]}]
  (build-log/debug-format "The directories `%s` are cleaned" dirs)
  (if (nil? (build-files/delete-files dirs)) build-exit-codes/ok build-exit-codes/catch-all))
