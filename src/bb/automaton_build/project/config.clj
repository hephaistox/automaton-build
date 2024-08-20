(ns automaton-build.project.config
  "Loads `project` configuration file."
  (:refer-clojure :exclude [read])
  (:require
   [automaton-build.os.edn-utils-bb :as build-edn]
   [automaton-build.os.filename     :as build-filename]))

(def schema
  "Project configuration schema"
  [:map {:closed true}
   [:app-name :string]
   [:code {:optional true}
    [:map {:closed true}
     [:forbidden-words [:vector :string]]]]
   [:monorepo
    [:map-of
     :keyword
     [:map {:closed true}
      [:apps
       [:vector
        [:map {:closed true}
         [:app-dir :string]]]]]]]])

(def project-cfg-filename "project.edn")

(defn filename
  "Returns the `project.edn` filename of the project in `app-dir`."
  [app-dir]
  (build-filename/create-file-path app-dir project-cfg-filename))

(defn read-from-dir
  "Returns the project configuration file descriptor in `app-dir`."
  [app-dir]
  (-> app-dir
      filename
      build-edn/read-edn))
