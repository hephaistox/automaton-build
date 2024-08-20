(ns automaton-build.project.shadow
  "Shadow-cljs.edn proxy."
  (:refer-clojure :exclude [read])
  (:require
   [automaton-build.os.edn-utils-bb :as build-edn]
   [automaton-build.os.filename     :as build-filename]))

(def shadow-cljs-filename "shadow-cljs.edn")

(defn filename
  "Returns the `shadow-cljs.edn` filename of the project in `app-dir`."
  [app-dir]
  (build-filename/create-file-path app-dir shadow-cljs-filename))

(defn read
  "Returns the file descriptor of the file called `filename`."
  [filename]
  (build-edn/read-edn filename))

(defn read-from-dir
  "Returns the project configuration file descriptor in `app-dir`."
  [app-dir]
  (-> app-dir
      filename
      read))

(defn build
  "Returns builds defined in`shadow-cljs-edn``."
  [shadow-cljs-edn]
  (some-> shadow-cljs-edn
          (get :builds)
          keys
          vec))
