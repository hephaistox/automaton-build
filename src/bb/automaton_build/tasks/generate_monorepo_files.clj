(ns automaton-build.tasks.generate-monorepo-files
  (:require
   [automaton-build.echo.headers             :refer [h1-error h1-error! h1-valid! normalln]]
   [automaton-build.monorepo.apps            :as build-apps]
   [automaton-build.monorepo.deps-edn        :as monorepo-deps-edn]
   [automaton-build.monorepo.files-css       :as monorepo-files-css]
   [automaton-build.monorepo.package-json    :as monorepo-package-json]
   [automaton-build.monorepo.shadow-cljs     :as monorepo-shadow-cljs]
   [automaton-build.monorepo.tailwind-config :as monorepo-tailwind-config]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.edn-utils             :as build-edn]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.filename              :as build-filename]
   [automaton-build.project.deps             :as build-deps]
   [automaton-build.project.map              :as build-project-map]
   [automaton-build.project.package-json     :as project-package-json]
   [automaton-build.project.shadow           :as build-project-shadow]
   [automaton-build.project.tailwind-config  :as project-tailwind-config]))

(def cli-opts-data
  (-> [["-d" "--deps" "Don't generate deps" :default true :parse-fn not]
       ["-s" "--shadow" "Don't generate shadow-cljs" :default true :parse-fn not]
       ["-t" "--tailwind" "Don't generate tailwind config" :default true :parse-fn not]
       ["-p" "--package" "Don't generate package-json" :default true :parse-fn not]
       ["-c" "--css" "Don't generate custom css" :default true :parse-fn not]]
      (concat build-cli-opts/help-options build-cli-opts/inverse-options)))

(def cli-opts
  (-> cli-opts-data
      build-cli-opts/parse-cli
      (build-cli-opts/inverse [:deps :shadow :tailwind :package :css])))

(defn create-monorepo-deps
  [monorepo-dir main-deps-edn subapps static-paths test-runner-configs]
  (let [all-paths (monorepo-deps-edn/generate-paths monorepo-dir subapps)
        src-paths (monorepo-deps-edn/src-paths all-paths)
        test-aliases (map (fn [{:keys [regex]
                                :as test-runner-config}]
                            (-> test-runner-config
                                (select-keys [:alias :match])
                                (assoc :paths
                                       (monorepo-deps-edn/filter-test-paths all-paths regex))))
                          test-runner-configs)]
    (-> (reduce #(monorepo-deps-edn/assoc-test-runner-alias %1 %2) main-deps-edn test-aliases)
        (assoc-in [:aliases :cljs-deps :extra-paths] all-paths)
        (assoc :deps (monorepo-deps-edn/generate-deps subapps))
        (assoc :paths (into static-paths src-paths)))))

(defn deps-edn-status
  [monorepo-project-map]
  (let [monorepo-project-map (-> monorepo-project-map
                                 build-project-map/add-deps-edn
                                 (build-apps/apply-to-subprojects build-project-map/add-deps-edn))
        subapps (:subprojects monorepo-project-map)
        monorepo-dir (:app-dir monorepo-project-map)
        main-deps-edn (get-in monorepo-project-map [:deps :edn])
        static-paths (get-in
                      monorepo-project-map
                      [:project-config-filedesc :edn :monorepo :generate-deps :paths :static])
        test-runner-configs (get-in
                             monorepo-project-map
                             [:project-config-filedesc :edn :monorepo :generate-deps :test-runner])]
    (if-let [new-monorepo-deps (create-monorepo-deps monorepo-dir
                                                     main-deps-edn
                                                     subapps
                                                     static-paths
                                                     test-runner-configs)]
      (if-let [res (build-deps/write
                    monorepo-dir
                    (str ";; This file is automatically updated by generate-deps-edn task\n"
                         new-monorepo-deps))]
        {:status :fail
         :exception res}
        {:status :ok})
      {:status :fail
       :exception "Generate deps-edn failed"})))

(defn shadow-cljs-status
  [monorepo-project-map]
  (let [monorepo-project-map (-> monorepo-project-map
                                 build-project-map/add-shadow-cljs
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-shadow-cljs))
        monorepo-dir (:app-dir monorepo-project-map)
        main-shadow-cljs (get-in monorepo-project-map [:shadow-cljs :edn])
        subapps (filter (fn [app] (some? (get-in app [:shadow-cljs :edn])))
                        (:subprojects monorepo-project-map))]
    (if-let [new-monorepo-shadow-cljs (monorepo-shadow-cljs/generate-shadow-cljs main-shadow-cljs
                                                                                 subapps)]
      (if-let [res (build-edn/write
                    (build-project-shadow/filename monorepo-dir)
                    (str ";; This file is automatically updated by generate-shadow-cljs task\n"
                         new-monorepo-shadow-cljs))]
        {:status :fail
         :exception res}
        {:status :ok})
      {:status :fail
       :exception "Generate shadow-cljs failed"})))

(defn tailwind-config-status
  [monorepo-project-map]
  (let [monorepo-project-map (-> monorepo-project-map
                                 build-project-map/add-tailwind-config
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-tailwind-config))
        monorepo-dir (:app-dir monorepo-project-map)
        main-tailwind-config (get-in monorepo-project-map [:tailwind-config :raw-content])
        subapps (filter (fn [app] (some? (get-in app [:tailwind-config :raw-content])))
                        (:subprojects monorepo-project-map))]
    (if-let [new-monorepo-tailwind-config
             (monorepo-tailwind-config/generate-tailwind-config main-tailwind-config subapps)]
      (if-let [res (-> (build-filename/create-file-path monorepo-dir
                                                        project-tailwind-config/tailwind-config-js)
                       (build-file/write-file new-monorepo-tailwind-config))]
        {:status :fail
         :exception res}
        {:status :ok})
      {:status :fail
       :exception "Generate tailwind-config failed"})))

(defn package-json-status
  [monorepo-project-map]
  (let [monorepo-project-map (-> monorepo-project-map
                                 build-project-map/add-package-json
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-package-json))
        monorepo-dir (:app-dir monorepo-project-map)
        main-package-json (get-in monorepo-project-map [:package-json :json])
        subapps (filter (fn [app] (some? (get-in app [:package-json :json])))
                        (:subprojects monorepo-project-map))]
    (if-let [new-monorepo-package-json
             (monorepo-package-json/generate-package-json main-package-json subapps)]
      (if-let [res (project-package-json/write-package-json monorepo-dir new-monorepo-package-json)]
        {:status :fail
         :exception res}
        {:status :ok})
      {:status :fail
       :exception "Generate package-json failed"})))

(defn custom-css-status
  [monorepo-project-map]
  (let [monorepo-project-map (-> monorepo-project-map
                                 (build-apps/apply-to-subprojects build-project-map/add-custom-css))
        monorepo-dir (:app-dir monorepo-project-map)
        custom-css-path (get-in monorepo-project-map [:project-config-filedesc :edn :frontend :css])
        subapps (filter (fn [app] (some? (get-in app [:custom-css :raw-content])))
                        (:subprojects monorepo-project-map))]
    (if-let [new-custom-css (monorepo-files-css/generate-custom-css subapps)]
      (if-let [res (-> (build-filename/create-file-path monorepo-dir custom-css-path)
                       (build-file/write-file
                        (str
                         "/* This file is automatically updated by `generate-custom-css` task */ \n"
                         new-custom-css)))]
        {:status :fail
         :exception res}
        {:status :ok})
      {:status :fail
       :exception "Generate custom-css failed"})))

(defn generate-files
  [monorepo-project-map]
  (let [deps-edn-status (if (get-in cli-opts [:options :deps])
                          [:deps-edn (deps-edn-status monorepo-project-map)]
                          [:deps-edn {:status :skipped}])
        shadow-cljs-status (if (get-in cli-opts [:options :shadow])
                             [:shadow-cljs (shadow-cljs-status monorepo-project-map)]
                             [:shadow-cljs {:status :skipped}])
        tailwind-config-status (if (get-in cli-opts [:options :tailwind])
                                 [:tailwind-config (tailwind-config-status monorepo-project-map)]
                                 [:tailwind-config {:status :skipped}])
        package-json-status (if (get-in cli-opts [:options :tailwind])
                              [:package-json (package-json-status monorepo-project-map)]
                              [:package-json {:status :skipped}])
        custom-css-status (if (get-in cli-opts [:options :tailwind])
                            [:custom-css (custom-css-status monorepo-project-map)]
                            [:custom-css {:status :skipped}])]
    [deps-edn-status
     shadow-cljs-status
     tailwind-config-status
     package-json-status
     custom-css-status]))

(defn run-monorepo
  []
  (normalln "Monorepo files generation...")
  (let [app-dir ""
        monorepo-name :default
        monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config
                                 (build-apps/add-monorepo-subprojects monorepo-name)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config))
        monorepo-dir (:app-dir monorepo-project-map)]
    (if (get-in monorepo-project-map [:project-config-filedesc :invalid?])
      (h1-error "No project file found for monorepo.")
      (if (nil? monorepo-dir)
        (h1-error "Files can't be updated as `monorepo-dir` is missing")
        (let [status (generate-files monorepo-project-map)]
          (doseq [[k v :as stat] status]
            (cond
              (= (:status v) :fail) (h1-error! k " - failed with message: " (:exception v))
              (= (:status v) :ok) (h1-valid! k " - success")
              (= (:status v) :skipped) (normalln k " - skipped")
              :else (h1-error! "Malformed output: " stat)))
          (if (every? (fn [[_k v]] (or (= (:status v) :ok) (= (:status v) :skipped))) status)
            (do (h1-valid! "Monorepo files generated successfull") 0)
            (do (h1-error! "Some of files generation has failed") 1)))))))
