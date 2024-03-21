(ns automaton-build.code-helpers.antq
  "Proxy to antq library to help with update of dependencies"
  (:require
   [antq.api]
   [automaton-build.app.bb-edn              :as build-bb-edn]
   [automaton-build.app.deps-edn            :as build-deps-edn]
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]))

(defn exclude-libraries
  "Returns `libs` without those that name is in `excluded-libs-names`"
  [libs excluded-libs-names]
  (reduce
   (fn [acc dep]
     (if (some #(= % (:name dep)) excluded-libs-names) acc (conj acc dep)))
   []
   libs))

(defn- append-dir
  "Takes a `libs` collection of maps and assoc `dir` under `:file`"
  [libs dir]
  (map (fn [lib]
         {:dependency lib
          :file (build-deps-edn/deps-path dir)})
       libs))

(defn do-update
  "Update the dependencies in deps.edn and bb.edn

   Params:
   * `exclude-libs` - coll of strings of libraries to exclude from update
   * `dir` - string where dependencies should be updated"
  [exclude-libs dir]
  (-> dir
      build-deps-edn/slurp
      antq.api/outdated-deps
      (exclude-libraries exclude-libs)
      (append-dir dir)
      antq.api/upgrade-deps!))

(defn generate-file-path
  [dir type]
  (case type
    :clojure [{:file (build-deps-edn/deps-path dir)}
              {:file (build-bb-edn/bb-edn-filename-fullpath dir)}]
    :pom [{:file (build-pom-xml/pom-xml dir)}]))

#_(defn add-project-paths
    [project-dir types]
    (concat (map #(add-file-path project-dir %) types)))

(defn add-dependency
  [lib version file-map]
  (assoc file-map
         :dependency
         {:name lib
          :latest-version version}))

(defn update-deps [deps-to-update] (antq.api/upgrade-deps! deps-to-update))
