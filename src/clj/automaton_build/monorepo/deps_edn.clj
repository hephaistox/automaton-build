(ns automaton-build.monorepo.deps-edn
  "Manage the monorepo dependencies"
  (:require
   [automaton-core.adapters.deep-merge :as deep-merge]
   [automaton-core.adapters.deps-edn :as deps-edn]
   [automaton-core.adapters.log :as log]

   [automaton-build.apps :as apps]
   [automaton-core.adapters.files :as files]))

(defn build-everything-deps
  "Build the monorepo `deps.edn` file.
  It start with the `deps.edn` file of the everything project, which is modified as such:
  * The `:paths` key is removed, as directory are starting from `clojure/everything`, they will be added again thanks to that namespace with the right path"
  [apps]
  (log/debug "Build everything deps")
  (let [everything-app (apps/everything apps)
        everything-deps (:deps-edn everything-app)]
    (when-not everything-deps
      (throw (ex-info "The everything project deps.edn is not found, aborting"
                      {:apps apps
                       :everything-app everything-app})))
    (dissoc everything-deps :paths)))

(defn- update-app-src
  "The paths in the collection pathv are updated so the directory of the app is a prefix"
  [app-dir pathv]
  (->> pathv
       (map (fn [src-item]
              (files/create-dir-path app-dir src-item)))
       dedupe
       sort
       (into [])))

(defn build-everything-path
  "Add all `path` directories of all projects (cust-app or not) in the everything app.
  Concerns `src` and `tests` directories.
  The function adds a prefix with the name of the app, so the source files will be found from `clojure/name-of-the-app/src` dir
  :run aliases are excluded."
  [apps]
  {:paths (vec (mapcat
                (fn [{:keys [app-dir deps-edn] :as _app}]
                  (->> (deps-edn/extract-paths deps-edn
                                               #{:run})
                       (update-app-src app-dir)))
                apps))})

(defn build-deps
  "Creates the list of dependencies.
  Add dependencies from `:deps-edn` of each project and `:deps-edn` and `:extra-deps` of all aliases`"
  [apps]
  {:deps (into {}
               (mapcat
                (fn [{:keys [deps-edn] :as _app}]
                  (deps-edn/extract-deps deps-edn))
                apps))})

(defn add-build-alias
  "Add the build alias"
  [apps]
  (let [build (apps/build apps)
        {build-project-deps-edn :deps-edn
         deps-edn :deps-edn
         app-dir :app-dir} build
        build-project-deps-edn (update (update build-project-deps-edn
                                               :paths (partial update-app-src app-dir))
                                       :extra-paths (partial update-app-src app-dir))]
    (when-not build-project-deps-edn
      (throw (ex-info (str "The `" deps-edn "` file is missing")
                      {:apps apps})))
    {:aliases {:build build-project-deps-edn}}))

(defn create-build-deps-edn
  "Build the `clojure/deps.edn` map
  Params:
  * `apps` applications"
  [apps]
  (let [deps-edn (apply deep-merge/deep-merge
                        (map (fn [f]
                               (f apps))
                             [build-everything-deps
                              build-everything-path
                              build-deps
                              add-build-alias]))
        libs (apps/get-libs apps)]
    (log/trace "Remove libs " (apply str libs))
    (deps-edn/remove-deps deps-edn
                          libs)))

(defn build-save-deps-edn
  "Build and save the build project `deps.edn` file
  Params:
  * `apps` applications"
  [apps]
  (deps-edn/update-deps-edn "."
                            (constantly (create-build-deps-edn apps))))
