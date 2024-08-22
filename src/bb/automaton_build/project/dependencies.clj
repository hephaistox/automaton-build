(ns automaton-build.project.dependencies
  "Managing project dependencies versions"
  (:require
   [automaton-build.project.impl.clj-deps :as build-project-clj]
   [automaton-build.project.impl.npm-deps :as build-project-npm]
   [clojure.string                        :as str]))

(defn type-schema
  "Type of dependency registry"
  []
  [:or [:clj-dep :keyword] [:npm :keyword]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn dependency-schema
  "Defines required keys for dependency update.
   Requires:
   * `type` defined in type-schema
   * `file` path to a file to update
   * `name` a name of the dependency to update
   * `version` a new version to set
   * `current-version` current version
   "
  []
  [:map
   [:type (type-schema)]
   [:path :string]
   [:name :string]
   [:current-version :string]
   [:version :string]])

(defn find-outdated-clj-deps
  "Return map with `:deps` key and a list of outdated maven dependencies in `app-dir` that are outdated.
   In case of an error returns map with `:err` and `:msg`"
  [app-dir]
  (-> app-dir
      build-project-clj/clj-outdated-deps
      (update :deps
              #(map (fn [dep]
                      (build-project-clj/clj-dep->dependency app-dir dep))
                    %))))

(defn find-outdated-npm-deps
  "Returns a map with `:deps` key and a list of outdated npm dependencies. In case of an error returns map with `:err`
   Using npm for versioning. Npm versioning cheatsheet https://gist.github.com/jonlabelle/706b28d50ba75bf81d40782aa3c84b3e"
  [app-dir]
  (-> app-dir
      build-project-npm/outdated-npm-deps
      (update :deps
              #(map (fn [dep]
                      (build-project-npm/npm-dep->dependency app-dir dep))
                    %))))

(defn hephaistox-pre-release?
  "Hardcoded check for hephaistox dependency. In future this could be replaced with a regex check defined based on project.edn excluded dependency"
  [dep]
  (and (str/starts-with? (str (:name dep)) "org.clojars.hephaistox")
       (str/includes? (str (:version dep)) "-")))

(defn exclude-deps
  "Returns `deps` without those that name is in `excluded-libs-names` and hephaistox deps pre-release versions."
  [deps excluded-deps-names]
  (remove (fn [dep]
            (some #(or (= (:name %) (:name dep)) (hephaistox-pre-release? dep))
                  excluded-deps-names))
          deps))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn update-dep!
  "Update single `dep` conforming to `dependency-schema`"
  [dep]
  (case (:type dep)
    :clj-dep (build-project-clj/update-dep! dep)
    :npm (build-project-npm/update-dep! dep)))

(defn update-deps!
  "Update all `deps` in `dir` (`deps` should conform to `dependency-schema`).
   Similar to `update-dep!` fn, but more performant in case of multiple deps to update."
  [dir deps]
  (let [{:keys [clj-dep npm]} (group-by #(:type %) deps)
        res [(build-project-clj/update-deps! dir clj-dep)
             (build-project-npm/update-npm-deps! dir npm)]]
    (when-not (every? #(nil? %) res) (some #(when (:error %) %) res))))
