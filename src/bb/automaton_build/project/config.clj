(ns automaton-build.project.config
  "Loads `project` configuration file."
  (:refer-clojure :exclude [read])
  (:require
   [automaton-build.os.file      :as build-file]
   [automaton-build.wf.edn-utils :as build-wf-edn]))

(def schema "Project configuration schema" [:map])

(def project-cfg-filename "project.edn")

(defn filename
  "Returns the `project.edn` filename of the project in `app-dir`."
  [app-dir]
  (build-file/create-file-path app-dir project-cfg-filename))

(defn read
  "Returns the content of the file called `filename`."
  [filename]
  (some-> filename
          build-file/is-existing-file?
          build-wf-edn/read-edn))
