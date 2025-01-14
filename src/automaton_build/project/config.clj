(ns automaton-build.project.config
  "Loads `project` configuration file."
  (:refer-clojure :exclude [read])
  (:require
   [automaton-build.os.edn-utils :as build-edn]
   [automaton-build.os.filename  :as build-filename]))

(def schema
  "Project configuration schema"
  [:map {:closed true}
   [:app-name :string]
   [:frontend {:optional true}
    [:map
     [:run-aliases {:optional true}
      [:vector :keyword]]
     [:css {:optional true}
      :string]]]
   [:code {:optional true}
    [:map {:closed true}
     [:forbidden-words [:vector :string]]]]
   [:deps {:optional true}
    [:map {:closed true}
     [:excluded-libs [:vector :map]]]]
   [:versions {:optional true}
    [:map {:closed true}
     [:bb :string]
     [:clj :string]
     [:jdk :string]
     [:npm :string]]]
   [:monorepo
    [:map-of
     :keyword
     [:map {:closed true}
      [:gha
       [:map {:closed true}
        [:version :string]]]
      [:generate-deps {:optional true}
       [:map
        [:paths [:map [:static [:vector :string]]]]
        [:test-runner [:vector [:map [:alias :keyword] [:match :string] [:regex :string]]]]]]
      [:apps
       [:vector
        [:map {:closed true}
         [:app-dir :string]
         [:repo-url :string]
         [:as-lib :symbol]]]]]]]
   [:publication {:optional true}
    [:map
     [:base-branch :string]
     [:la-branch :string]
     [:clojars {:optional true}
      :boolean]
     [:cc {:optional true}
      :boolean]
     [:excluded-aliases {:optional true}
      [:vector :keyword]]
     [:pom-xml-license {:optional true}
      [:map [:name :string] [:url :string]]]]]])

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
