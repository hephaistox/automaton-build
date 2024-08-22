(ns automaton-build.project.impl.npm-deps
  (:require
   [automaton-build.os.cmds     :refer [blocking-cmd]]
   [automaton-build.os.filename :as build-filename]
   [cheshire.core               :as json]
   [clojure.string              :as str]))

(defn update-dep!
  "Update `dep`"
  [{:keys [name version path]
    :as _dep}]
  (blocking-cmd ["npm" "install" (str name "@" version) "--save"] path))

(defn update-npm-deps!
  "Update all `deps` in `dir`. Returns nil when successful otherwise a map with `:error`"
  [dir deps]
  (when (seq deps)
    (let [res (blocking-cmd
               ["npm"
                "install"
                "--prefix"
                "."
                (str/join " "
                          (mapv (fn [dep] (str (:name dep) "@" (:version dep)))
                                deps))]
               dir)]
      (when-not (= 0 (:exit res))
        {:error (:err res)
         :data res}))))

(defn- npm-install-check
  "NPM install is required because npm outdated will return malformed output if the packages are not installed in node_modules"
  [dir]
  (let [npm-install (blocking-cmd ["npm" "install"] dir)]
    (when (not= 0 (:exit npm-install))
      {:err {:message "Npm install failed"
             :dir dir
             :data npm-install}})))

(defn npm-dep->dependency
  "Turns npm outdated result string line to a dependency conforming to `dependency-schema`
   Example line
  node_modules/sentry:@sentry/browser@7.119.0:@sentry/browser@8.26.0:@sentry/browser@8.26.0:clojure
  Which follows pattern path:current-package:wanted-version:latest-version:depended-by"
  [dir [lib-name values]]
  (let [file-path (build-filename/create-file-path dir "package.json")
        cur-version (get values "current")
        version (get values "latest")]
    {:file file-path
     :type :npm
     :name lib-name
     :current-version cur-version
     :version version}))

(defn outdated-npm-deps
  "Returns map with `:deps` and list of dependencies. In case of an error returns map with `:err`"
  [dir]
  (try (if-let [check-err (npm-install-check dir)]
         check-err
         (let [res (blocking-cmd ["npm" "outdated" "--prefix" "." "-json"] dir)]
           (case (:exit res)
             0 (assoc res :deps [])
             1 (assoc res :deps (json/parse-string (:out res)) :exit 0)
             (assoc res :msg "NPM outdated deps search failed"))))
       (catch Exception e {:err e})))
