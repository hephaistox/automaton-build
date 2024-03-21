(ns automaton-build.code-helpers.antq
  "Proxy to antq library to help with update of dependencies"
  (:require
   [antq.api]
   [automaton-build.app.bb-edn              :as build-bb-edn]
   [automaton-build.app.deps-edn            :as build-deps-edn]
   [automaton-build.cicd.deployment.pom-xml :as build-pom-xml]
   [automaton-build.os.files                :as build-files]))

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
    :clojure [{:file (build-deps-edn/deps-path dir)
               :type :clojure}
              {:file (build-bb-edn/bb-edn-filename-fullpath dir)
               :type :clojure}]
    :pom [{:file (build-pom-xml/pom-xml dir)
           :type :pom}]))

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

(comment
  (require '[automaton-build.cicd.server :as build-cicd-server])
  (build-cicd-server/update-workflows
   ["cust_app/landing/.github/workflows/commit_validation.yml"]
   "1.1.3-la"
   "gha-landing")
  (build-files/is-existing-file?
   "cust_app/landing/.github/workflows/commit_validation.yml")
  (update-deps [{:file
                 "cust_app/landing/.github/workflows/commit_validation.yml"
                 :dependency {:name "hephaistox/landing"
                              :project :github-action
                              :type :github-tag
                              :latest-version "1.1.3-la"}}]))
