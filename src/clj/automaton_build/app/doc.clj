(ns automaton-build.app.doc
  "Build the documentation of an application"
  (:require
   [automaton-build.adapters.doc :as doc]
   [automaton-build.app :as app]
   [automaton-build.adapters.frontend-compiler :as frontend_compiler]))

(defn build-doc
  "Generate the documentation for the application `app`
  * `app` is the application to build doc from"
  [{:keys [app-name app-dir doc?]
    :as app}]
  (when doc?
    (doc/build-doc app-name
                   app-name
                   app-name
                   app-dir
                   (app/get-existing-src-dirs app))))

(defn size-optimization-report
  "Generate the documentation for the application `app`
  * `app` is the application to build doc from"
  [{:keys [app-dir frontend?]
    :as _app}]
  (when frontend?
    (frontend_compiler/create-size-optimization-report app-dir)))
