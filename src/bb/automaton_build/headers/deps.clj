(ns automaton-build.tasks.impl.headers.deps
  "Load deps edn with headers logs."
  (:require
   [automaton-build.echo.headers :refer [errorln uri-str]]
   [automaton-build.os.filename  :as build-filename]
   [automaton-build.project.deps :as build-deps]))

(defn deps-edn
  "Load the `app-dir` `deps.edn`.
  Return `nil` if invalid, the deps content otherwise."
  [app-dir]
  (let [deps (build-deps/deps-edn app-dir)
        deps-edn (:edn deps)]
    (if (:invalid? deps)
      (do (errorln "No valid `deps.edn` found in directory "
                   (uri-str (-> app-dir
                                build-deps/deps-edn-filename
                                build-filename/absolutize)))
          nil)
      deps-edn)))

(defn save-deps
  "Save the `deps-edn-content` in `app-dir`.
  Return `nil` if invalid, the deps content otherwise."
  [deps-edn-content app-dir]
  (try (spit (build-deps/deps-edn-filename app-dir) deps-edn-content) (catch Exception _ nil)))
