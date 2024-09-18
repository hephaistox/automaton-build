(ns automaton-build.tasks.impl.headers.deps
  "Load deps edn with headers logs."
  (:require
   [automaton-build.echo.headers :refer [errorln uri-str]]
   [automaton-build.project.deps :as build-deps]))

(defn deps-edn
  "Load the `app-dir` `deps.edn`.
  Return `nil` if invalid, the deps content otherwise."
  [app-dir]
  (let [deps (build-deps/deps-edn app-dir)
        deps-edn (:edn deps)]
    (if (:invalid? deps)
      (do (errorln "No valid `deps.edn` found in directory " (uri-str app-dir) "") nil)
      deps-edn)))
