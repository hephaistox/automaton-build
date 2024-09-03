(ns automaton-build.code-helpers.update-deps
  "Code to help with project dependencies update"
  (:require
   [automaton-build.app.bb-edn                     :as build-bb-edn]
   [automaton-build.app.deps-edn                   :as build-deps-edn]
   [automaton-build.cicd.deployment.pom-xml        :as build-pom-xml]
   [automaton-build.code-helpers.antq              :as build-code-helpers-antq]
   [automaton-build.code-helpers.compiler.shadow   :as build-compiler-shadow]
   [automaton-build.code-helpers.frontend-compiler :as build-frontend-compiler]
   [automaton-build.log                            :as build-log]
   [automaton-build.os.files                       :as build-files]))

(defn type-schema
  "Type of dependency file to update"
  []
  [:or [:deps-edn :keyword] [:bb-edn :keyword] [:pom-xml :keyword]])

(defn dependency-schema
  "Defines information about dependency to update. It's name and new version to set."
  []
  [:map [:name :string] [:latest-version :string]])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn update-dep-schema
  "Defines required keys for dependency update.
   Requires:
   * `type` defined in type-schema
   * `file` path to a file to update
   * `dependency` defined in dependency-schema"
  []
  [:map [:type (type-schema)] [:file :string] [:dependency (dependency-schema)]])

(defn add-dependency
  [lib version file-map]
  (assoc file-map
         :dependency
         {:name lib
          :latest-version version}))

(defn add-type [type file-map] (assoc file-map :type type))

(defn add-file-path
  [path file-map]
  (case (:type file-map)
    :deps-edn (assoc file-map :file (build-deps-edn/deps-path path))
    :bb-edn (assoc file-map :file (build-bb-edn/bb-edn-filename-fullpath path))
    :pom-xml (assoc file-map :file (build-pom-xml/pom-xml path))))

(defn update-frontend-deps!
  [app-dir]
  (if (build-frontend-compiler/is-frontend-project? app-dir)
    (do (build-log/info "Updating npm libraries...")
        (if (= :ok (build-compiler-shadow/npm-install app-dir))
          (= :ok (build-compiler-shadow/npm-update app-dir))
          (do (build-log/warn "Npm projects version update failed") false)))
    (do (build-log/info "Frontend update deps skipped") true)))

(defn update-deps! [deps-maps] (build-code-helpers-antq/update-deps! deps-maps))

(defn exclude-libraries
  "Returns `libs` without those that name is in `excluded-libs-names`"
  [dep-maps excluded-libs-names]
  (reduce
   (fn [acc dep-map]
     (if (some #(= % (:name (:dependency dep-map))) excluded-libs-names) acc (conj acc dep-map)))
   []
   dep-maps))

(defn find-outdated-deps-edn
  "Finds outdated deps in deps.edn file"
  [app-dir]
  (->> app-dir
       build-deps-edn/slurp
       build-code-helpers-antq/find-outdated-deps
       (map #(add-dependency (:name %) (:latest-version %) {}))
       (map #(add-type :deps-edn %))
       (map #(add-file-path app-dir %))))

(defn update-app-deps
  "Update all dependencies in `app-dir` excluding `exclude-libs`"
  [app-dir exclude-libs]
  (build-log/info "Updating npm libraries...")
  (if (update-frontend-deps! app-dir)
    (do (build-log/info "Updating deps.edn file")
        (-> (find-outdated-deps-edn app-dir)
            (exclude-libraries exclude-libs)
            update-deps!)
        true)
    (do (build-log/warn "Npm projects version update failed") false)))

(defn find-project-files-to-update
  [app-dir]
  (->> [:deps-edn :bb-edn :pom-xml]
       (map #(add-type % {}))
       (map #(add-file-path app-dir %))
       (filter #(build-files/is-existing-file? (:file %)))))
