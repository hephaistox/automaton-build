(ns automaton-build.app.package-json
  (:require
   [automaton-build.os.files  :as build-files]
   [automaton-build.os.json   :as build-json]
   [automaton-build.utils.map :as build-utils-map]))

(def package-json "package.json")

(defn package-json-path
  [& app-dir]
  (-> (apply build-files/create-dir-path app-dir)
      (build-files/create-file-path package-json)
      build-files/absolutize))

(defn compare-package-json-deps
  "Returns the one with higher version"
  [deps1 deps2]
  (if (pos? (compare (second deps1) (second deps2))) deps2 deps1))

(defn get-dependencies
  [package-json]
  (select-keys
   package-json
   ["dependencies" "devDependencies" :dependencies :devDependencies]))

(defn add-dependencies
  "Adds 'dependencies' and 'devDependencies' from `deps` map onto a `package-json` map."
  ([package-json deps]
   (assoc package-json
          "dependencies"
          (apply merge-with
                 compare-package-json-deps
                 (map #(or (get % "dependencies") (:dependencies %)) deps))
          "devDependencies" (apply merge-with
                                   compare-package-json-deps
                                   (map #(or (get % "devDependencies")
                                             (:devDependencies %))
                                        deps)))))

(defn load-package-json
  "Read the package.json from dir.
   Returns the content of a file as a clojure map.
  Params:
  * `dir` the directory of the application"
  [dir]
  (let [package-filepath (package-json-path dir)]
    (when (build-files/is-existing-file? package-filepath)
      (build-json/read-file package-filepath))))

(defn write-package-json
  "Saves package-json content to a json file in `target-dir` with `content`."
  [target-dir content]
  (build-json/write-file (package-json-path target-dir)
                         (build-utils-map/sorted-map-nested content)))
