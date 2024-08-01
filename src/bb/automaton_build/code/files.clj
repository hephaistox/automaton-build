(ns automaton-build.code.files
  "Access coding file."
  (:require
   [automaton-build.app-data.deps :as build-deps]
   [automaton-build.os.file       :as build-file]))

(defn project-dirs
  "Returns project directories as defined in the `deps` (whatever the alias)."
  [deps]
  (->> deps
       build-deps/get-src
       (keep build-file/is-existing-dir?)
       (mapv str)))

(defn project-files
  "Based on a `project-dirs`, returns a map of filename and file content."
  [project-dirs]
  (->> project-dirs
       (mapcat (fn [dir]
                 (-> dir
                     (build-file/matching-files "**{.clj,.cljc,.cljs,.edn}"))))
       (mapv str)
       (mapv (fn [f]
               {:filename f
                :content (try (build-file/read-file f)
                              (catch Exception _ nil))}))))
