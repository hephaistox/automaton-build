(ns automaton-build.app-data
  "Helpers manipulating an application through its `app-data`"
  (:require
   [automaton-build.app-data.impl :as build-app-data-impl]
   [automaton-build.file-repo.clj-code :as build-clj-code]
   [automaton-build.os.files :as build-files]
   [clojure.string :as str]))

(defn classpath-dirs
  "Existing source directories for front and back, as strings of absolutized directories
  Params:
  * `app` is the app to get dir from"
  [app]
  (->> (concat (build-app-data-impl/clj-compiler-classpath app)
               (build-app-data-impl/cljs-compiler-classpaths app))
       sort
       dedupe
       vec))

(defn src-dirs
  "Existing source directories for front and back, as strings of absolutized directories
  Exclude resources
  Params:
  * `app` is the app to get dir from"
  [app]
  (->> (concat (build-app-data-impl/clj-compiler-classpath app)
               (build-app-data-impl/cljs-compiler-classpaths app))
       sort
       dedupe
       vec))

(defn project-paths-files
  "Returns a sequence of all files from app src path directories.
   Params:
   * `app` is the app to get the dir from"
  [app]
  (->> app
       src-dirs
       (filter build-files/is-existing-dir?)
       (mapcat (fn [dir] (build-clj-code/search-clj-filenames dir)))))

(defn project-search-files
  "Return the list of files present in the app directory
   Params:
   * `app-data`
   * `search-files` collection of files to search in a project"
  [app-dir search-files]
  (-> app-dir
      (build-files/search-files (str "**{" (str/join "," search-files) "}"))))

(defn project-root-dirs
  "Returns project root directories, will return one directory unless project has multiple projects inside then it will return directories.
  The project is defined by existence of build_config.edn file.
  Params:
  * `app-dir`"
  [app-dir]
  (build-files/parent-dirs-of-files (project-search-files
                                     (build-files/absolutize app-dir)
                                     ["build_config.edn"])))

(defn project-paths-files-content
  [app]
  (-> app
      project-paths-files
      build-clj-code/make-clj-from-files))
