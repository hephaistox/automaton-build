(ns automaton-build.code-helpers.update-deps-clj
  (:require
   [automaton-build.app.bb-edn   :as build-bb-edn]
   [automaton-build.app.deps-edn :as build-deps-edn]
   [automaton-build.utils.map    :as build-utils-map]))

(defn update-single-dep
  "In `app-dir` update `dep` with `val`.  Returns true when successful."
  [dir dep val]
  (let [bb-edn-file (build-bb-edn/slurp dir)
        deps-file (build-deps-edn/slurp dir)
        deps-content (build-utils-map/update-k-v deps-file dep val)]
    (every? some?
            [(build-deps-edn/spit dir deps-content)
             (build-bb-edn/spit dir bb-edn-file deps-content)])))
