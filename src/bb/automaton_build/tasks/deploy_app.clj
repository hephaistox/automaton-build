(ns automaton-build.tasks.deploy-app
  "Deployment app per app"
  (:require
   [automaton-build.code.vcs                :as build-vcs]
   [automaton-build.echo.headers            :refer [h1 h2 h2-error! h2-valid! normalln]]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.project.map             :as build-project-map]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd]]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(def ^:private cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def verbose? (get-in cli-opts [:options :verbose]))

(def help (get-in cli-opts [:options :help]))

; ********************************************************************************
; *** Task
; ********************************************************************************

(defn run
  []
  (let [app-dir ""
        project-map (-> (build-project-map/create-project-map app-dir)
                        build-project-map/add-project-config
                        build-project-map/add-deps-edn)
        {:keys [_app-dir _project-config-filedesc app-name]} project-map]
    (h1 (str "Deploy app `" app-name "`"))
    (h2 "Check run status")
    (let [{:keys [status run-id]}
          (-> (build-vcs/gh-run-wip?-cmd)
              (blocking-cmd "" "Error when getting github run status" verbose?)
              build-vcs/gh-run-wip?-analyze)]
      (normalln "Run-id " run-id " is " status)
      (when (= :wip status)
        (h2-error! "Run" run-id "has not finished")
        (normalln (str "Visit `https://github.com/hephaistox/"
                       app-name
                       "/actions/runs/"
                       run-id
                       "` if needed"))
        (normalln (str "or execute `gh run watch " run-id "`"))
        (System/exit build-exit-codes/invalid-state))
      (when (= :run-failed status)
        (h2-error! "Run has failed")
        (normalln
         (str "Visit `https://github.com/hephaistox/" app-name "/actions/runs/" run-id "`"))
        (normalln (str "or execute `gh run view --log " run-id "`"))
        (System/exit build-exit-codes/invalid-state))
      (h2-valid! "Run" run-id "passed"))))
