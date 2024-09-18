(ns automaton-build.app-data.impl
  (:require
   [automaton-build.app.deps-edn                   :as build-deps-edn]
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.os.files                       :as build-files]))

(defn clj-compiler-classpath
  "Return absolutized directories of sources of `app`, only if they already exist!
  Params:
  * `app` is the app to get app-dir and deps-edn content map from"
  [{:keys [app-dir deps-edn]
    :as _app}]
  (let [paths (build-deps-edn/extract-paths deps-edn #{})]
    (apply build-files/sorted-absolutize-dirs app-dir paths)))

(defn cljs-compiler-classpaths
  "Existing source directories for frontend, as strings of absolutized directories"
  [{:keys [app-dir shadow-cljs]
    :as _app}]
  (->> shadow-cljs
       build-frontend-compiler/extract-paths
       (apply build-files/sorted-absolutize-dirs app-dir)))
