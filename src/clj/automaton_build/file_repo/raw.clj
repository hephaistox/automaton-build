(ns automaton-build.file-repo.raw
  "Repository of files associating the name of a file to its content
  This repo doesn't assume anything about the file content, it just slurp it"
  (:require
   [automaton-build.file-repo.raw.impl :as build-raw-impl]))

(defprotocol FileRepo
  (exclude-files [this exclude-files]
   "Return a `FileRepo` without the files in `excluded-files`\n Params:\n * `exclude-files` sequence of file names that will not be included in the filename")
  (file-repo-map [this]
   "The map associating filepath with their content")
  (nb-files [this]
   "Return the number of embedded files")
  (filter-by-extension [this extensions]
   "Filter the repo on files matching the extension\n Params: \n * `extensions`")
  (filter-repo [this filter-fn]
   "Filter the repo files\n Params: \n * `repo` repository to filter \n * `filter-fn` function taking the pair [filename file-content] as an argument and returning true if we want to keep that file in the repo. If nil is provided, the whole repo is returned"))

(defrecord RawFileRepo [_file-repo-map]
  FileRepo
    (exclude-files [_ excluded-files]
      (-> _file-repo-map
          (build-raw-impl/exclude-files excluded-files)
          ->RawFileRepo))
    (file-repo-map [_] _file-repo-map)
    (filter-repo [_ filter-fn]
      (-> (build-raw-impl/filter-repo-map _file-repo-map filter-fn)
          ->RawFileRepo))
    (nb-files [_] (count _file-repo-map))
    (filter-by-extension [_ extensions]
      (-> _file-repo-map
          (build-raw-impl/filter-by-extension extensions)
          ->RawFileRepo)))

(defn make-raw-file-repo
  "Return a map of filename associated with their content, e.g.
  `{\"core.clj\" \"core.clj file content\"}`
  Params:
  * `filenames` list of filename to include in the repo, non existing files will be skipped"
  [filenames]
  (->RawFileRepo (build-raw-impl/repo-map filenames)))
