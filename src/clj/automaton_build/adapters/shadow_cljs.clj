(ns automaton-build.adapters.shadow-cljs
  (:require
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.edn-utils :as edn-utils]))

(def shadow-cljs-edn
  "shadow-cljs.edn")

(defn load-shadow-cljs
  "Read the shadow-cljs of an app
  Params:
  * `dir` the directory where to
  Returns the content as data structure"
  [dir]
  (edn-utils/read-edn-or-nil (files/create-file-path dir
                                                     shadow-cljs-edn)))

(defn extract-paths
  "Extract paths from the shadow cljs file content
  Params:
  * `shadow-cljs-content` is the content of a shadow-cljs file
  Return a flat vector of all source paths"
  [shadow-cljs-content]
  (:source-paths shadow-cljs-content))
