(ns automaton-build.headers.edn-utils
  "Load deps edn with headers logs."
  (:refer-clojure :exclude [load])
  (:require
   [automaton-build.echo.headers :refer [errorln]]
   [automaton-build.os.edn-utils :as build-edn]
   [automaton-build.os.filename  :as build-filename]))

(defn load
  "Load file `filename`.
  Return `nil` if invalid, the deps content otherwise."
  [filename]
  (let [deps (build-edn/read-edn filename)
        deps-edn (:edn deps)]
    (if (:invalid? deps)
      (do (errorln "File `"
                   (-> filename
                       build-filename/absolutize)
                   "` found in directory ")
          nil)
      deps-edn)))

(defn save-deps
  "Save the `deps-edn-content` in `app-dir`.
  Return `nil` if invalid, the deps content otherwise."
  [deps-edn-content filename]
  (try (spit filename deps-edn-content) (catch Exception _ nil)))
