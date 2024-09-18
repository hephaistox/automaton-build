(ns automaton-build.tasks.deps-version
  "Task for managing outdated dependencies"
  (:require
   [automaton-build.echo.headers         :refer [clear-prev-line
                                                 h1
                                                 h1-error
                                                 h1-error!
                                                 h1-valid
                                                 h2
                                                 h2-valid
                                                 normalln]]
   [automaton-build.monorepo.apps        :as build-apps]
   [automaton-build.os.cli-opts          :as build-cli-opts]
   [automaton-build.os.text              :as build-text]
   [automaton-build.project.dependencies :as build-dependencies]
   [automaton-build.project.map          :as build-project-map]
   [clojure.pprint                       :as pp]
   [clojure.string                       :as str]))

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(defn- wrap-outdated-report
  [{:keys [exit deps]
    :as rep}]
  (let [deps (remove nil? deps)]
    (if (= 0 exit)
      (if (empty? deps)
        {:status :up-to-date}
        {:status :outdated
         :deps deps})
      {:status :error
       :err rep})))

(defn outdated-maven-report
  "Returns outdated maven deps for `app-dir`, if all deps are up-to-date returns nil, otherwise returns map with :error"
  [app-dir]
  (let [{:keys [status deps err msg]
         :as rep}
        (-> app-dir
            build-dependencies/find-outdated-clj-deps
            wrap-outdated-report)]
    (case status
      :up-to-date nil
      :outdated deps
      :error {:error {:msg msg
                      :e err
                      :data rep}}
      {:error {:msg "Unknown issue while analyzing outdated dependencies"
               :data rep}})))

(defn outdated-npm-report
  "Returns outdated npm deps for `app-dir`, if all deps are up-to-date returns nil, otherwise returns map with :error"
  [app-dir]
  (let [{:keys [status deps]
         :as rep}
        (-> app-dir
            build-dependencies/find-outdated-npm-deps
            wrap-outdated-report)]
    (case status
      :up-to-date nil
      :outdated deps
      :error {:error {:msg (:msg rep)
                      :data rep}}
      {:error {:msg "Unknown issue while analyzing outdated dependencies"
               :data rep}})))

(defn outdated-deps-report
  "Returns map with
   :status :done if there is no outdated-deps.
   :status :found and :deps when there are outdated dependencies
   :status :error when there was some problem"
  [app-dir excluded-deps]
  (let [maven-report (outdated-maven-report app-dir)
        npm-report (outdated-npm-report app-dir)
        reports (remove nil? [maven-report npm-report])
        deps (build-dependencies/exclude-deps (distinct (flatten reports)) excluded-deps)]
    (if (some #(some? (:error %)) reports)
      (first reports)
      (if (or (empty? reports) (empty? deps))
        {:status :done}
        {:status :found
         :deps deps}))))

(defn- clear-table-cli [table-height] (print (str/join "" (repeat table-height clear-prev-line))))

(defn run*
  "Update dependencies core logic"
  [{:keys [app-name app-dir project-config-filedesc]}]
  (h1 app-name ": dependencies update")
  (h2 "Outdated deps analysis... ")
  (let [excluded-deps (get-in project-config-filedesc [:edn :deps :excluded-libs])
        deps-to-update (outdated-deps-report app-dir excluded-deps)]
    (case (:status deps-to-update)
      :done (do (print (str build-text/move-oneup build-text/clear-eol))
                (h1-valid app-name ": dependencies update")
                nil)
      :found (let [outdated-deps-table (with-out-str (pp/print-table
                                                      [:name :current-version :version]
                                                      (:deps deps-to-update)))
                   outdated-deps-table-height (+ 1 (count (re-seq #"\n" outdated-deps-table)))]
               (h2-valid "Outdated deps found: " outdated-deps-table)
               (h2 "Updating outdated deps...")
               (let [update (build-dependencies/update-deps! app-dir (:deps deps-to-update))]
                 (clear-table-cli outdated-deps-table-height)
                 (if (:error update)
                   (do (h1-error app-name
                                 ": deps update failed with error: "
                                 (with-out-str (pp/pprint update)))
                       update)
                   (do (h1-valid app-name " : dependencies update") nil))))
      (do (print (str build-text/move-oneup build-text/clear-eol))
          (h1-error app-name
                    ": there was an error during outdated deps analysis : "
                    (with-out-str (pp/pprint deps-to-update)))
          deps-to-update))))


(defn run
  "Start task for a single project"
  []
  (-> "."
      build-project-map/create-project-map
      build-project-map/add-project-config
      run*))


(defn run-monorepo
  "Start task for all projects in monorepo"
  []
  (normalln "Starting monorepo apps deps update")
  (normalln)
  (let [app-dir ""
        monorepo-name :default
        monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config
                                 (build-apps/add-monorepo-subprojects monorepo-name)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config))]
    (if (get-in monorepo-project-map [:project-config-filedesc :invalid?])
      (h1-error! "No project file found for monorepo.")
      (let [projects (:subprojects monorepo-project-map)]
        (if (empty? projects)
          (h1-error! "No monorepo apps has been found")
          (do
            (doseq [project projects] (run* project))
            (normalln
             "[temporary] to update also files in the monorepo root, run `bb heph-task generate-files`")))))))
