(ns automaton-build.tasks.la-without-opts
  (:require
   [automaton-build.la :as build-la]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map
   {:keys [task-registry]
    :as _app-data}]
  (build-la/run task-registry nil))
