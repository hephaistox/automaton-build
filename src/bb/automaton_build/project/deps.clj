(ns automaton-build.project.deps
  "Project `deps.edn` file."
  (:require
   [automaton-build.os.edn-utils-bb :as build-edn]
   [automaton-build.os.filename     :as build-filename]))

(defn deps-edn
  "Read project `deps.edn`."
  [app-dir]
  (-> (build-filename/create-file-path app-dir "deps.edn")
      build-edn/read-edn))

(defn get-src
  "Returns source directories."
  [deps-edn]
  (->> deps-edn
       :aliases
       vals
       (mapcat :extra-paths)
       (concat (:paths deps-edn))
       (filterv #(re-find #"src|test" %))))
