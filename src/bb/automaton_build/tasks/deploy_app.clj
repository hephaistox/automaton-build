(ns automaton-build.tasks.deploy-app
  "Deployment app per app"
  (:require
   [automaton-build.code.vcs                :as build-vcs]
   [automaton-build.echo.headers            :refer
                                            [errorln h1 h2 h2-error h2-error! h2-valid normalln]]
   [automaton-build.os.cli-opts             :as build-cli-opts]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.project.map             :as build-project-map]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd]]))

; ********************************************************************************
; *** Task setup
; ********************************************************************************

(def ^:private cli-opts
  (-> [["-t" "--tag TAG" "Tag name, e.g. 1.3.2"]
       ["-m" "--message MESSAGE" "Message for local apps push"]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def verbose? (get-in cli-opts [:options :verbose]))

(def help (get-in cli-opts [:options :help]))

(def tag (get-in cli-opts [:options :tag]))

(def message (get-in cli-opts [:options :message]))

; ********************************************************************************
; *** Task
; ********************************************************************************

(defn run
  []
  (let [app-dir ""
        project-map (-> (build-project-map/create-project-map app-dir)
                        build-project-map/add-project-config)
        {:keys [app-name]} project-map]
    (h1 (str "Deploy app `" app-name "`"))
    (when-not tag (errorln "The tag is mandatory"))
    (when-not message (errorln "The message is mandatory"))
    (when (:errors cli-opts) (errorln "Arguments are invalid") (build-cli-opts/error-msg cli-opts))
    (when (or (some? (:errors cli-opts)) (some nil? [tag message]))
      (-> (build-cli-opts/print-usage cli-opts "deploy-app")
          normalln)
      (System/exit build-exit-codes/invalid-argument))
    (h2 "Check run status")
    (let [{:keys [status run-id]}
          (-> (build-vcs/gh-run-wip?-cmd)
              (blocking-cmd app-dir "Error when getting github run status" verbose?)
              build-vcs/gh-run-wip?-analyze)]
      (when (= :wip status)
        (h2-error "Run" run-id "has not finished")
        (normalln (str "Visit `https://github.com/hephaistox/"
                       app-name
                       "/actions/runs/"
                       run-id
                       "` if needed"))
        (normalln (str "or execute `gh run watch " run-id "`"))
        (System/exit build-exit-codes/invalid-state))
      (when (= :run-failed status)
        (h2-error "Run has failed")
        (normalln
         (str "Visit `https://github.com/hephaistox/" app-name "/actions/runs/" run-id "`"))
        (normalln (str "or execute `gh run view --log " run-id "`"))
        (System/exit build-exit-codes/invalid-state))
      (h2-valid "Run" run-id "passed")
      (h2 "Tag" tag (str " with message `" message "`"))
      (when-not (= 0
                   (-> ["git" "tag" "-a" tag "-m" (str "\"" message "\"")]
                       (blocking-cmd app-dir "Tagging commit has failed" verbose?)
                       :exit))
        (h2-error! "Tag has failed, deployment abort")
        (System/exit build-exit-codes/cannot-execute))
      (when-not (= 0
                   (-> ["git" "push" "origin" tag]
                       (blocking-cmd app-dir "Tag push has failed" verbose?)
                       :exit))
        (h2-error! "Tag push has failed, deployment abort")
        (System/exit build-exit-codes/cannot-execute))
      (h2-valid "Commit is tagged with " tag))))
