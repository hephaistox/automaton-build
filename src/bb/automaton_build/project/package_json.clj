(ns automaton-build.project.package-json
  (:require
   [automaton-build.data.map    :as build-data-map]
   [automaton-build.os.filename :as build-filename]
   [automaton-build.os.json-bb  :as build-json-bb]))

(def package-json "package.json")

(defn package-json-path [app-dir] (build-filename/create-file-path app-dir package-json))

(defn compare-package-json-deps
  "Returns the one with higher version"
  [deps1 deps2]
  (if (pos? (compare (second deps1) (second deps2))) deps2 deps1))

(defn get-dependencies
  [package-json]
  (select-keys package-json ["dependencies" "devDependencies" :dependencies :devDependencies]))

(defn add-dependencies
  "Adds 'dependencies' and 'devDependencies' from `deps` map onto a `package-json` map."
  ([package-json deps]
   (-> package-json
       (assoc "dependencies"
              (apply merge-with
                     compare-package-json-deps
                     (map #(or (get % "dependencies") (:dependencies %)) deps)))
       (assoc "devDependencies"
              (apply merge-with
                     compare-package-json-deps
                     (map #(or (get % "devDependencies") (:devDependencies %)) deps))))))

(defn load-package-json
  "Read the package.json from dir.
   Returns the content of a file as a clojure map.
  Params:
  * `dir` the directory of the application"
  [dir]
  (let [package-filepath (package-json-path dir)] (build-json-bb/read-file package-filepath)))

(defn write-package-json
  "Saves package-json content to a json file in `target-dir` with `content`."
  [target-dir content]
  (build-json-bb/write-file (package-json-path target-dir)
                            (build-data-map/sorted-map-nested content)))
