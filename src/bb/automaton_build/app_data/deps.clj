(ns automaton-build.app-data.deps
  "Project deps."
  (:require
   [automaton-build.os.file      :as build-file]
   [automaton-build.wf.edn-utils :as build-wf-edn]
   [clojure.string               :as str]))

(defn deps-edn
  "Read project `deps.edn`."
  [app-dir]
  (-> (build-file/create-file-path app-dir "deps.edn")
      build-wf-edn/read-edn))

(defn get-src
  "Returns source directories."
  [deps-edn]
  (->> deps-edn
       :aliases
       vals
       (mapcat :extra-paths)
       (concat (:paths deps-edn))
       (filterv #(str/includes? % "src"))))

(comment
  (-> (deps-edn "")
      get-src)
  ;
)
