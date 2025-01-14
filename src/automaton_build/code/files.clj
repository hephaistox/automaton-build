(ns automaton-build.code.files
  "Access code files."
  (:require
   [automaton-build.os.file      :as build-file]
   [automaton-build.os.filename  :as build-filename]
   [automaton-build.project.deps :as build-deps]))

(defn project-dirs
  "Returns project directories path (as strings), as defined in the `deps-edn` content - whatever the alias."
  [deps-edn-dir deps-edn]
  (->> deps-edn
       build-deps/get-src
       (mapv (partial build-filename/create-dir-path deps-edn-dir))
       (keep build-file/is-existing-dir?)
       (map str)
       set))

(defn project-files
  "Based on a `project-dirs`, returns the filenames matching `files-extensions`.
   `files-extensions` defaults to all files with extensions: .clj/cljc/.cljs/.edn"
  ([project-dirs files-extensions]
   (->> project-dirs
        (mapcat (fn [project-dir]
                  (-> project-dir
                      (build-file/matching-files files-extensions))))
        (mapv str)))
  ([project-dirs] (project-files project-dirs "**{.clj,.cljc,.cljs,.edn}")))
