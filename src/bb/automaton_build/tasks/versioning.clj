(ns automaton-build.tasks.versioning
  (:require
   [automaton-build.code.formatter          :as build-formatter]
   [automaton-build.echo.headers            :refer [build-writter
                                                    clear-prev-line
                                                    h1
                                                    h1-error
                                                    h1-error!
                                                    h1-valid
                                                    h1-valid!
                                                    h2
                                                    h2-error!
                                                    h2-valid!
                                                    normalln]]
   [automaton-build.monorepo.apps           :as build-apps]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.version              :as build-version]
   [automaton-build.project.dependencies    :as build-dependencies]
   [automaton-build.project.deps            :as build-deps]
   [automaton-build.project.map             :as build-project-map]
   [automaton-build.project.pom-xml         :as build-project-pom-xml]
   [automaton-build.project.versioning      :as build-project-versioning]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd success]]
   [clojure.pprint                          :as pp]
   [clojure.string                          :as str]))

(def cli-opts
  (-> [["-e"
        "--env ENVIRONMENT"
        "env variable e.g. la, production"
        :missing
        "Environment is required, run -e la or -e production"
        :parse-fn
        #(keyword %)]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(defn update-version
  [app-dir app-name target-env]
  (if-let [version (build-project-versioning/generate-new-app-version target-env app-dir app-name)]
    (if-let [_save-version (build-version/save-version app-dir version)]
      {:status :success
       :version version}
      {:status :failed})
    {:status :skipped}))

(defn generate-pom-xml
  [app-dir deps-edn excluded-aliases as-lib license version]
  (let [source-paths (build-deps/extract-paths deps-edn excluded-aliases)]
    (if-let [pom-res
             (build-project-pom-xml/generate-pom-xml as-lib source-paths app-dir license version)]
      {:status :failed
       :res pom-res}
      {:status :success})))

(defn update-deps-version
  "Aligns all apps dependencies in `apps-dirs` to have newest specific app version. So if it's run for automaton-build, all files containing it as dependency will be updated to it's version.edn version"
  [as-lib version]
  (when as-lib
    {:name as-lib
     :version version
     :type :clj-dep}))

(defn app-result-summary
  [app-name
   {:keys [status]
    :as res}]
  (cond
    (= :success status) (h1-valid app-name " success")
    (= :skipped status) (do (print clear-prev-line) (h1 app-name " skipped"))
    :else (h1-error app-name " failed with: " res)))

(defn versioning-update
  [env subapps]
  (h1 "Monorepo apps version setting")
  (mapv (fn [app]
          (let [app-dir (:app-dir app)
                app-name (:app-name app)
                version-res (update-version app-dir app-name env)]
            (print (str/join "" (repeat 8 clear-prev-line)))
            (app-result-summary app-name version-res)
            (assoc app :version-update version-res)))
        subapps))

(defn pom-xml-status
  [app]
  (if (= :success (get-in app [:version-update :status]))
    (let [excluded-aliases (get-in app
                                   [:project-config-filedesc :edn :publication :excluded-aliases])
          as-lib (get-in app [:project-config-filedesc :edn :publication :as-lib])
          pom-xml-license (get-in app [:project-config-filedesc :edn :publication :pom-xml-license])
          deps-edn (get-in app [:deps :edn])
          app-dir (:app-dir app)
          pom-xml-res (if pom-xml-license
                        (generate-pom-xml app-dir
                                          deps-edn
                                          excluded-aliases
                                          as-lib
                                          pom-xml-license
                                          (get-in app [:version-update :version]))
                        {:status :skipped})]
      pom-xml-res)
    {:status :skipped}))

(defn pom-xml-generation
  [subapps]
  (h1 "Monorepo apps pom.xml generation")
  (mapv (fn [app]
          (normalln (:app-name app) " start pom-xml generation")
          (let [status (pom-xml-status app)]
            (app-result-summary (:app-name app) status)
            (assoc app :pom-xml status)))
        subapps))

(defn align-subapps-deps
  [subapps]
  (h1 "Monorepo apps dependencies update of set versions")
  (let [apps-to-update
        (remove nil?
                (mapv (fn [app]
                        (let [as-lib (get-in app
                                             [:project-config-filedesc :edn :publication :as-lib])]
                          (when (and as-lib (= :success (get-in app [:version-update :status])))
                            (update-deps-version as-lib (get-in app [:version-update :version])))))
                      subapps))]
    (if (empty? apps-to-update)
      (normalln "no apps found for depenedency update")
      (mapv (fn [{:keys [app-dir]
                  :as app}]
              (normalln (:app-name app) " dependencies update started")
              (let [deps-update-res (build-dependencies/update-deps! "" app-dir apps-to-update)
                    status (if (:error deps-update-res)
                             {:status :failed
                              :res deps-update-res}
                             {:status :success})]
                (app-result-summary (:app-name app) status)
                (assoc app :subapps-update status)))
            subapps))))

(defn- detailed-report
  [name result]
  (cond
    (= :failed (:status result)) (h2-error! name " failed with: " (with-out-str (pp/pprint result)))
    (= :skipped (:status result)) (h2 name " skipped")
    (= :success (:status result)) (h2-valid! name " success")))

(defn summary-details
  [{:keys [version-update subapps-update pom-xml app-name]}]
  (h2 app-name " details:")
  (detailed-report "Version update" version-update)
  (detailed-report "Dependencies update" subapps-update)
  (detailed-report "POM-XML generation" pom-xml))

(defn format-files
  "Format project files."
  [project-dir]
  (h1 "Format project files.")
  (let [s (build-writter)
        res (binding [*out* s]
              (-> (build-formatter/format-clj-cmd)
                  (blocking-cmd project-dir "" false)))]
    (if (success res) (h1-valid "Format project files.") (h1-error "Format project files."))))

(defn run-monorepo
  "Versioning for monorepo has 3 steps:
   1. Set version for each project
   2. Generate new pom.xml
   3. Update other subapps references of updated project"
  []
  (let [monorepo-project-map (-> (build-project-map/create-project-map "")
                                 build-project-map/add-project-config
                                 (build-apps/add-monorepo-subprojects :default)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config
                                  build-project-map/add-deps-edn))
        env (get-in cli-opts [:options :env])
        subapps (->> monorepo-project-map
                     :subprojects
                     (versioning-update env)
                     pom-xml-generation
                     align-subapps-deps)]
    (format-files (:app-dir monorepo-project-map))
    (normalln)
    (normalln)
    (h1 "Synthesis of results:")
    (normalln)
    (normalln)
    (mapv (fn [{:keys [subapps-update pom-xml version-update app-name]
                :as app}]
            (let [results [version-update subapps-update pom-xml]]
              (cond
                (every? (fn [{:keys [status]}] (= :success status)) results)
                (h1-valid! app-name " success" " ver." (:version version-update))
                (every? (fn [{:keys [status]}] (= :skipped status)) results) (normalln app-name
                                                                                       " skipped")
                (some (fn [{:keys [status]}] (= :failed status)) results)
                (do (h1-error! app-name " failed") (summary-details app))
                :else (do (h1-valid! app-name " success") (summary-details app)))))
          subapps)
    0))
