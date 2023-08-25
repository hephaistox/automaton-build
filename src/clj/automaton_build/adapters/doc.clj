(ns automaton-build.adapters.doc
  "Code documentation creation
  Proxy to codox"
  (:require
   [codox.main :as codox]

   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-core.env-setup :as env-setup]))

(defn doc-subdir
  "The directory of the documentation
  Params:
  * `app-dir` the root directory of the app"
  [app-dir]
  (files/create-dir-path app-dir (get-in env-setup/env-setup
                                         [:documentation :codox])))

(defn build-doc
  "Generate the documentation
  Params:
  * `app-name` application name
  * `doc-title` the title of the documentation generated
  * `doc-description` the description
  * `app-dir` the root directory of the app
  * `app-dirs` where the source should be looked at"
  [app-name doc-title doc-description app-dir app-dirs]
  (log/info "Build application documentation cust-app `" doc-title "` in directory `" app-name "`")
  (let [dir (doc-subdir app-dir)]
    (files/create-dirs dir)
    (codox/generate-docs {:name doc-title
                          :version "1.0"
                          :source-paths app-dirs
                          :output-path dir
                          :description doc-description})))
