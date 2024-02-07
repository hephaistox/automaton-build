(ns automaton-build.file-repo.clj-code
  "Repository of files associating the name of a file to its content
  This repo deal with text files and will split the file in vector of lines"
  (:require
   [automaton-build.file-repo.text :as build-filerepo-text]
   [automaton-build.file-repo.raw.impl :as build-raw-impl]
   [automaton-build.file-repo.raw :as build-filerepo-raw]
   [automaton-build.os.files :as build-files]
   [clojure.string :as str]))

;; Match a usage of the code and list all concerned extensions
(defonce ^:private usage-to-extension
  {:clj [".clj"]
   :cljs [".cljs"]
   :cljc [".cljc"]
   :edn [".edn"]
   :clj-compiler [".clj" ".cljc"]
   :cljs-compiler [".cljc" ".cljs"]
   :code [".clj" ".cljc" ".cljs"]
   :reader [".clj" ".cljc" ".cljs" ".edn"]})

(def all-reader-extensions
  "All extensions understood by a clojure reader"
  (get usage-to-extension :reader))

(defonce ^:private glob-code-extensions
  (format "**{%s}" (str/join "," all-reader-extensions)))

(defprotocol CodeRepo
  (filter-by-usage [this usage-kw]
   "Filter the existing files based on its usage, see `code-extenstions-map` for details"))

(defrecord CljCodeFileRepo [file-repo-map*]
  build-filerepo-raw/FileRepo
    (exclude-files [_ exclude-files]
      (-> (build-raw-impl/exclude-files file-repo-map* exclude-files)
          ->CljCodeFileRepo))
    (file-repo-map [_] file-repo-map*)
    (nb-files [_] (count file-repo-map*))
    (filter-repo [_ filter-fn]
      (-> (build-raw-impl/filter-repo-map file-repo-map* filter-fn)
          ->CljCodeFileRepo))
    (filter-by-extension [_ extensions]
      (build-raw-impl/filter-by-extension file-repo-map* extensions))
  CodeRepo
    (filter-by-usage [_ usage-kw]
      (build-raw-impl/filter-repo-map
       file-repo-map*
       (fn [[filename _]]
         (some some?
               (map (partial build-files/match-extension? filename)
                    (get usage-to-extension usage-kw)))))))

(defn search-clj-filenames
  "Return the list of clojure code file names
   Params:
  * `dir` the directory where files are searched"
  [dir]
  (build-files/search-files dir glob-code-extensions))

(defn- match
  [filenames extensions-kw]
  (let [extensions (mapcat (fn [extension-kw]
                             (get usage-to-extension extension-kw))
                    extensions-kw)]
    (filter (fn [filename]
              (apply build-files/match-extension? filename extensions))
            filenames)))

(defn map-files-content
  [files & usage-ids]
  (let [usage-ids (or usage-ids [:reader])]
    (-> files
        (match usage-ids)
        build-filerepo-text/make-text-file-map)))

(defn make-clj-from-files
  "Maps `files` paths with their content, accepts only files with extensions defined in `usage-ids` (that defaults to :reader)"
  [files & usage-ids]
  (let [usage-ids (or usage-ids [:reader])]
    (-> (apply map-files-content files usage-ids)
        ->CljCodeFileRepo)))

(defn make-clj-repo-from-dirs
  "Build the repo while searching clj files in the directory `dir`
  Limit to the given extensions
  Params:
  * `dirs`
  * `usage-ids` (Optional, default to `reader`) list of usage accepted, according to usage-to-extension definition"
  [dirs & usage-ids]
  (let [usage-ids (or usage-ids [:reader])]
    (-> (fn [dir]
          (apply map-files-content (search-clj-filenames dir) usage-ids))
        (mapcat dirs)
        ->CljCodeFileRepo)))

(defn filenames
  "Returns sequence of file path strings from CljCodeFileRepo instance"
  [clj-files-repo]
  (keys (:file-repo-map* clj-files-repo)))
