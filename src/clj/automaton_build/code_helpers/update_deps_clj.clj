(ns automaton-build.code-helpers.update-deps-clj
  (:require
   [antq.core]
   [automaton-build.app.bb-edn :as build-bb-edn]
   [automaton-build.app.deps-edn :as build-deps-edn]
   [automaton-build.log :as build-log]
   [automaton-build.utils.map :as build-utils-map]
   [clojure.string :as str]))

(defn do-update
  "Update the depenencies.

   Params:
   * `excluded-libs` - coll of strings of libraries to exclude from update
   * `dir` - string or coll of strings where dependencies should be updated"
  [excluded-libs & dirs]
  (let [dirs-param (format "--directory=%s" (str/join ":" dirs))
        exclude-params (map #(str "--exclude=" %) excluded-libs)]
    (apply antq.core/-main "--upgrade" dirs-param exclude-params)))

(defn update-single-dep
  "In `app-dir` update `dep` with `val`.  Returns true when successful."
  [dir dep val]
  (let [bb-edn-file (build-bb-edn/slurp dir)
        deps-file (build-deps-edn/slurp dir)
        deps-content (build-utils-map/update-k-v deps-file dep val)]
    (build-log/trace-format "Deps-content: " (:bb-deps (:aliases deps-content)))
    (every? some?
            [(build-deps-edn/spit dir deps-content)
             (build-bb-edn/spit dir bb-edn-file deps-content)])))
