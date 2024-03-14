(ns automaton-build.code-helpers.antq
  "Proxy to antq library to help with update of dependencies"
  (:require
   [antq.core]
   [clojure.string :as str]))

(defn do-update
  "Update the dependencies in deps.edn and bb.edn

   Params:
   * `excluded-libs` - coll of strings of libraries to exclude from update
   * `dir` - string or coll of strings where dependencies should be updated"
  [excluded-libs & dirs]
  (let [dirs-param (format "--directory=%s" (str/join ":" dirs))
        exclude-params (map #(str "--exclude=" %) excluded-libs)]
    (apply antq.core/-main "--upgrade" "--skip=pom" dirs-param exclude-params)))
