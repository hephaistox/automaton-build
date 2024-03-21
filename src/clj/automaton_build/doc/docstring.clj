(ns automaton-build.doc.docstring
  "Code documentation creation
  Proxy to codox"
  (:require
   [automaton-build.log      :as build-log]
   [automaton-build.os.files :as build-files]
   [codox.main               :as codox]))

(defn docstring
  "Generate the documentation
  Params:
  * `doc-title` title of the document
  * `doc-description` decscription of the document"
  [app-dir app-name app-dirs doc-title doc-description doc-dir]
  (build-log/info "Code documentation")
  (build-log/trace-map "Build doc with parameters"
                       :app-dir app-dir
                       :app-name app-name
                       :doc-title doc-title
                       :doc-description doc-description
                       :doc-dir doc-dir)
  (build-log/trace-data app-dirs "Directories search for code are")
  (build-log/info-format
   "Build application documentation cust-app `%s` in directory `%s`"
   doc-title
   app-name)
  (let [dir (build-files/create-dir-path app-dir doc-dir)]
    (build-files/create-dirs dir)
    (codox/generate-docs {:name doc-title
                          :version "1.0"
                          :source-paths app-dirs
                          :namespaces [#"clj[s|c]?$"]
                          :output-path dir
                          :description doc-description})
    true))
