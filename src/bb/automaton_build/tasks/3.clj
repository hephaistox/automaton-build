(ns automaton-build.tasks.3
  "Quick code check."
  (:require
   [automaton-build.echo.headers              :refer [h1-error! h1-valid! normalln]]
   [automaton-build.monorepo.apps             :as build-apps]
   [automaton-build.os.cli-opts               :as build-cli-opts]
   [automaton-build.project.map               :as build-project-map]
   [automaton-build.tasks.impl.commit         :as build-tasks-commit]
   [automaton-build.tasks.impl.linter         :as build-linter]
   [automaton-build.tasks.impl.report-aliases :as build-tasks-report-aliases]
   [automaton-build.tasks.impl.reports-fw     :as build-tasks-reports-fw]))



;; ********************************************************************************
;;
;; Cli options parametrization
;; ********************************************************************************
(def cli-opts-data
  (-> [["-l" "--lint" "Don't lint" :default true :parse-fn not]
       ["-r" "--reports" "Don't execute reports" :default true :parse-fn not]
       ["-C" "--wip-commit-message" "Default wip commit message"]
       ["-c" "--commit-message COMMIT-MESSAGE" "Commit message"]]
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              build-cli-opts/inverse-options)))
(def cli-opts
  (-> cli-opts-data
      build-cli-opts/parse-cli
      (build-cli-opts/inverse [:lint :reports])))

(def verbose
  "Set to true if the cli has been set up to `-v` verbose option."
  (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; Tasks
;; ********************************************************************************
(defn run-monorepo
  "Execute all tests and reports."
  []
  (normalln "Quick code check for a monorepo.")
  (normalln)
  (normalln)
  (let [app-dir ""
        monorepo-name :default
        commit-msg (if-let [commit-msg (get-in cli-opts [:options :commit-message])]
                     commit-msg
                     (get-in cli-opts [:options :wip-commit-message]))
        monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config
                                 build-project-map/add-deps-edn
                                 (build-apps/add-monorepo-subprojects monorepo-name)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config
                                  build-project-map/add-deps-edn))]
    (if (get-in monorepo-project-map [:project-config-filedesc :invalid?])
      (h1-error! "No project file found for monorepo.")
      (let [report-aliases-status (when (get-in cli-opts [:options :reports])
                                    (build-tasks-report-aliases/scan-alias monorepo-project-map
                                                                           verbose))
            fw-status (when (get-in cli-opts [:options :reports])
                        (build-tasks-reports-fw/report monorepo-project-map))
            linter-status (when (get-in cli-opts [:options :lint])
                            (build-linter/lint (:deps monorepo-project-map) verbose))
            commit-status (when commit-msg
                            (build-tasks-commit/commit
                             (:dir monorepo-project-map)
                             (if (->> [report-aliases-status linter-status fw-status]
                                      (remove nil?)
                                      (every? true?))
                               "wip - wf-3 ok"
                               "wip - wf-3 KO")
                             verbose))]
        (normalln)
        (h1-valid! "Synthesis:")
        (when-not (nil? linter-status) (build-linter/synthesis linter-status))
        (when-not (nil? report-aliases-status)
          (build-tasks-report-aliases/synthesis report-aliases-status))
        (when-not (nil? fw-status) (build-tasks-reports-fw/synthesis fw-status))
        (when-not (nil? commit-status) (build-tasks-commit/synthesis commit-status))
        (and commit-status linter-status report-aliases-status fw-status)))))
