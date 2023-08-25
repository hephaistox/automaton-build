(ns automaton-build.app
  "Application
  build.app and all subsequent namespaces are here to build one application"
  (:require
   [automaton-build.adapters.code-files :as code-files]
   [automaton-core.adapters.deps-edn :as deps-edn]
   [automaton-core.adapters.edn-utils :as edn-utils]
   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-build.adapters.schema :as schema]
   [automaton-build.adapters.shadow-cljs :as shadow-cljs]))

(def env-schema
  "Schema of an environment"
  [:map {:closed true}
   [:host-repo-address :string]
   [:remote-name :string]
   [:app-id :string]])

(def cust-app-schema
  "Customer application specific schema"
  [[:publication {:optional true} [:map {:closed true}
                                   [:repo-address :string]
                                   [:as-lib {:optional true} :symbol]
                                   [:branch :string]]]
   [:run-env {:optional true} [:map {:closed true}
                               [:test-env env-schema]
                               [:prod-env env-schema]]]
   [:templating {:optional true} [:map {:closed true}
                                  [:app-title :string]]]])

(def app-build-config-schema
  "Application schema"
  (into []
        (concat [:map {:closed true}
                 [:app-name :string]

                 [:build? {:optional true} :boolean]
                 [:cust-app? {:optional true} :boolean]
                 [:everything? {:optional true} :boolean]
                 [:template-app? {:optional true} :boolean]

                 [:doc? {:optional true} :boolean]
                 [:frontend? {:optional true} :boolean]

                 [:monorepo
                  [:map [:app-dir :string]]]]
                cust-app-schema)))

(defn validate
  "Validate the file build config matches the expected format
  Return an exception if not validated
  Params:
  * `app-build-config` content of the file to validate"
  [app-build-config]
  (schema/schema-valid-or-throw app-build-config-schema
                                app-build-config
                                (str "Build configuration of app "
                                     (:app-name app-build-config)
                                     " is not matching the schema")))

(defn get-deps-src-dirs
  "Return absolutized directories of sources of `app`
  Params:
  * `app` is the app to get dir from"
  [{:keys [app-dir deps-edn]
    :as _app}]
  (->> (deps-edn/extract-paths deps-edn)
       (mapv (comp files/absolutize (partial files/create-dir-path app-dir)))
       dedupe
       sort))

(defn get-existing-src-dirs
  "Existing source directories, as strings of absolutized directories
  Params:
  * `app` is the app to get dir from"
  [app]
  (files/filter-existing-dir (get-deps-src-dirs app)))
+;;TODO Search other usages of src-dir to check if it is only clj which is intended

(defn get-cljs-existing-src-dirs
  "Existing source directories for backend, as strings of absolutized directories"
  [app]
  (->> app
       :app-dir
       shadow-cljs/load-shadow-cljs
       shadow-cljs/extract-paths
       (mapv #(files/create-dir-path (:app-dir app) %))
       dedupe
       sort))

(defn get-clj*-existing-src-dirs
  "Existing source directories for both front and back, as strings of absolutized directories
  Params:
  * `app` is the app to get dir from"
  [app]
  (-> (concat (files/filter-existing-dir (get-deps-src-dirs app))
           (files/filter-existing-dir (get-cljs-existing-src-dirs app)))
      dedupe
      sort))

(defn search-in-codefiles
  "Search files and dir in the application code.
  Return the list of filenames matching pattern in the app
  Only directories set up in deps.edn or shadow-cljs are used
  Params:
  * `app` is the app where to search in"
  [app]
  (->> app
       get-clj*-existing-src-dirs
       (mapcat code-files/code-files-name)))

(defn build-app
  "Build an application
  Params:
  * `build-config-filename` the full path to the build configuration file to be loaded"
  [build-config-filename]
  (let [build-config (edn-utils/read-edn build-config-filename)
        deps-edn (deps-edn/load-deps-edn (files/extract-path build-config-filename))]
    (validate build-config)
    (assoc build-config
           :app-dir (files/extract-path build-config-filename)
           :deps-edn deps-edn)))

(defn update-project-deps
  "Update the application `app` with the commit id `commit-id` of lib `lib-app`.
  The update happens only if the application refer the lib already.

  * `app` is the application we need to update
  * `lib-app` is the dependency that has been updated
  * `commit-id` is the new commit id"
  [app lib-app commit-id]
  (let [{target-app-dir :app-dir
         target-app-name :app-name} app
        {lib-name :app-name} lib-app
        as-lib (get-in lib-app [:publication :as-lib])]
    (log/debug (format "Update project dependency `%s`, with library `%s`, commit = `%s`" target-app-name lib-name commit-id))
    (deps-edn/update-deps-edn target-app-dir
                              (partial deps-edn/update-commit-id as-lib commit-id))))

(defn is-cust-app-but-template?
  "True if `app-name` is matching a name from a customer application but template
  Params:
  * `app` application"
  [app]
  (and (:cust-app? app)
       (not (:template-app? app))))

(defn is-cust-app-but-everything?
  "True if `app-name` is matching a name from a customer application but everything
  Params:
  * `app` application"
  [app]
  (and (:cust-app? app)
       (not (:everything? app))))

(defn code-files-repo
  "Creates a code files repository for application app"
  [app]
  (->> app
       get-existing-src-dirs
       (map code-files/code-files-repo)
       (apply merge)))

(defn is-cust-app-or-everything?
  "Is the application a cust-app or everything project
  Params:
  * `app` the application to test"
  [app]
  (or (:cust-app? app)
      (:everything? app)))

;;TODO Is that function still useful
(defn create-code-files-map
  "Return the code files in the app, matching the `pattern`.
  Code files are understood like files matching the
  A map is built with the filename as a key, and the content as a value
  * `app` in which the files are searched for"
  [{:keys [app-name app-dir] :as _app}]
  (log/debug "Create files-map for `" app-name "`")
  (->> (search-in-codefiles app-dir)
       (filter files/is-existing-file?)
       (map (fn [filename]
              [(str (files/absolutize filename))
               (files/read-file (files/absolutize filename))]))
       (into {})))
