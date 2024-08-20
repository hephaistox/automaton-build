(ns automaton-build.tasks.impl.headers.shadow
  "Echo error message for shadow file manipulation."
  (:require
   [automaton-build.echo.headers   :refer [errorln]]
   [automaton-build.project.shadow :as build-project-shadow]))

(defn read-dir
  "Read the project `shadow-cljs.edn`, echo in terminal if an error occur."
  [project-dir]
  (let [file-desc (build-project-shadow/read-from-dir project-dir)
        success? (not (:invalid? file-desc))]
    (when-not success?
      (errorln
       "Unexpected error, shadow-cljs has not been found in project `project-dir`."))
    (:edn file-desc)))

(defn build
  "Returns builds defined in`shadow-cljs-edn`."
  [shadow-cljs-edn]
  (some-> shadow-cljs-edn
          (get :builds)
          keys
          vec))
