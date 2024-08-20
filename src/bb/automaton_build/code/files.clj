(ns automaton-build.code.files
  "Access code files."
  (:require
   [automaton-build.os.file      :as build-file]
   [automaton-build.os.filename  :as build-filename]
   [automaton-build.project.deps :as build-deps]))

(defn project-dirs
  "Returns project directories path (as strings), as defined in the `deps.edn` file - whatever the alias."
  [deps-edn-filedesc]
  (let [root-dir (:dir deps-edn-filedesc)
        deps (:edn deps-edn-filedesc)]
    (->> deps
         build-deps/get-src
         (mapv (fn [file] (build-filename/create-dir-path root-dir file)))
         (keep build-file/is-existing-dir?)
         (mapv str)
         set)))

(defn project-files
  "Based on a `project-dirs`, returns the filenames."
  [project-dirs]
  (->> project-dirs
       (mapcat (fn [project-dir]
                 (-> project-dir
                     (build-file/matching-files "**{.clj,.cljc,.cljs,.edn}"))))
       (mapv str)))
