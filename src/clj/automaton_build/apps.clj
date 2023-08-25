(ns automaton-build.apps
  "Gather the function for managing apps in the monorepo

  An application is one of the item managed by the monorepo.
  A customer application is one application that aims to be deployed once."
  (:require
   [clojure.set :as set]

   [automaton-build.app :as app]
   [automaton-core.adapters.string :as bas]))

;; *********************
;; Build apps
;; *********************

(defn validate
  "Validate all apps
  Params:
  * `apps` applications"
  [apps]
  (doseq [app apps]
    (app/validate app)))

(defn build-apps
  "Build the apps based on the list of build config filenames given as parameter
  Params:
  * `build-config-filenames` list of name of the build configuration file"
  [build-config-filenames]
  (map #(app/build-app %)
       build-config-filenames))

;; *********************
;; Search one app among apps
;; *********************

(defn search-for-one-key
  "Internal usage only, search exactly one application matching the key `kw`
  Params:
  * `kw` the keyword to search. That keyword should appear once in the apps
  * `apps` applications"
  [kw apps]
  (let [msg (format "Exactly one %s project is required" (bas/remove-last-character (name kw)))
        selected-apps (filter (fn [app]
                                (get app kw))
                              apps)]
    (when-not (= (count selected-apps)
                 1)
      (throw (ex-info msg
                      {:apps apps
                       :msg msg
                       :selected-app selected-apps})))
    (first selected-apps)))

(def template-app
  "Return the template application
  Params:
  * `apps` applications"
  (partial search-for-one-key :template-app?))

(def everything
  "Return the everything project
  Params:
  * `apps` applications"
  (partial search-for-one-key :everything?))

(def build
  "Return the build project
  Params:
  * `apps` applications"
  (partial search-for-one-key :build?))

(defn search-app-by-name
  "Search an app among `apps` with its name `app-name`
  Params:
  * `apps` applications
  * `app-name` the name `:app-name` should match"
  [apps app-name]
  (let [candidates (filter #(= app-name
                               (:app-name %))
                           apps)]
    (when-not (= (count candidates) 1)
      (throw (ex-info (format "Exactly one app called %s exists" app-name)
                      {:app-name app-name
                       :apps apps
                       :candidates candidates})))
    (first candidates)))

;; *********************
;; Subset of apps
;; *********************

(defn cust-apps
  "Set of customer apps
  Params:
  * `apps` applications"
  [apps]
  (->> apps
       (filter :cust-app?)
       set))

;; *********************
;; List of application name
;; *********************

(defn app-names
  "List all application names
  Params:
  * `apps` applications"
  [apps]
  (->> apps
       (map :app-name)
       set))

(defn cust-app-names
  "Set of customer application names
  Params:
  * `apps` applications"
  [apps]
  (->> apps
       cust-apps
       (map :app-name)
       set))

(defn app-but-everything-names
  "List of all application names but everything project
  Params:
  * `apps` applications"
  [apps]
  (set/difference (app-names apps)
                  #{(:app-name (everything apps))}))

(defn get-libs
  "Library names of all applications of the monorepo"
  [apps]
  (->> apps
       (map #(get-in % [:publication :as-lib]))
       dedupe
       (remove nil?)))

;; *********************
;; Check app compliance
;; *********************

(defn is-app?
  "True if `app-name` is matching an existing app
  Params:
  * `apps` applications
  * `app-name` name to check if it is an application"
  [apps app-name]
  (some #(= % app-name)
        (mapv :app-name
              apps)))

(defn is-app-but-everything?
  "True if `app-name` is matching an existing app except everything
  Params:
  * `apps` appl` applications
  * `app-name` name to check if it is cust-app or everything"
  [apps app-name]
  (and (is-app? apps app-name)
       (-> (search-app-by-name apps app-name)
           app/is-cust-app-but-everything?)))

(defn is-app-but-template?
  "True if `app-name` is matching an existing app except template?
  Params:
  * `apps` appl` applications
  * `app-name` name to check if it is cust-app or everything"
  [apps app-name]
  (and (is-app? apps app-name)
       (-> (search-app-by-name apps app-name)
           app/is-cust-app-but-template?)))

;; *********************
;; About dependency graph
;; *********************

(defn app-dependency-graph
  "Creates a graph dependency of our apps, i.e. a map associating the lib symbol to a map containing, e.g.:
  `{'hephaistox/automaton {:edges {'hephaistox/build {}}
                           :app-name \"automaton-app\"
                           :app-dir \"automaton_app\"}}`
  Params:
  * `apps` applications"
  [apps]
  (->> apps
       (map (fn [{:keys [app-name app-dir]
                  :as app}]
              [(or (get-in app [:publication :as-lib])
                   app-name)
               {:edges (into {}
                             (map (fn [dep]
                                    [(first dep) {}])
                                  (get-in app [:deps-edn :deps])))
                :app-name app-name
                :app-dir app-dir}]))
       (into {})))

(defn remove-not-required-apps
  "Remove in the graph the application which are not a dependency
  Params:
  * `graph` graph of dependencies"
  [graph]
  (select-keys graph
               (mapcat (fn [[_ app-data]]
                         (keys (:edges app-data)))
                       graph)))

(defn code-files-repo
  "Creates a code files repository for application app
    Params:
  * `apps` applications"
  [apps]
  (->> apps
       (map app/code-files-repo)
       (apply merge)))

(defn get-existing-src-dirs
  [apps]
  (vec (mapcat app/get-existing-src-dirs
               apps)))

(defn first-app-matching
  "Scan the apps until the first app returning true with (app-matching-fn? app) is met
  Params:
  * `apps` applications
  * `app-matching-fn?` function executed on app returning a boolean"
  [apps app-matching-fn?]
  (some #(and (app-matching-fn? %) %)
        apps))

(defn code-filenames-in-apps
  "Search files and dir in the applications code.
  Return the list of filenames matching pattern in the app
  Only directories set up in deps.edn or shadow-cljs are used
  Params:
  * `apps` applications"
  [apps]
  (vec (->> apps
            (mapcat app/search-in-codefiles))))
