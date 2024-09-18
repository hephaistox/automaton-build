(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.code.vcs               :as build-vcs]
   [automaton-build.echo.headers           :refer [h1-error! h1-valid! normalln]]
   [automaton-build.monorepo.apps          :as build-apps]
   [automaton-build.os.cli-input-bb        :as build-cli-input]
   [automaton-build.os.cli-opts            :as build-cli-opts]
   [automaton-build.os.cmds                :as build-commands]
   [automaton-build.os.file                :as build-file]
   [automaton-build.project.map            :as build-project-map]
   [automaton-build.tasks.impl.headers.vcs :as build-headers-vcs]
   [clojure.string                         :as str]))

(def cli-opts
  (-> [["-e"
        "--env ENVIRONMENT"
        "env variable e.g. la, production"
        :missing
        "Environment is required, run -e la or -e production"
        :parse-fn
        #(keyword %)]
       ["-f" "--force" "Do not ask about local apps push" :default true :parse-fn not]
       ["-m" "--message" "Message for local apps push" :parse-fn str]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))



(defn- pull-base-branch
  "Pull base branch, cannot be executed on base branch"
  [app-dir base-branch current-branch]
  (when-not (= current-branch base-branch)
    (-> (build-vcs/pull-changes-chain-cmd base-branch)
        (concat [[(build-vcs/merge-cmd base-branch current-branch)]])
        (build-commands/force-dirs app-dir)
        (build-commands/chain-cmds)
        build-commands/first-failing
        build-commands/success)))

(defn clean-state?
  [app-dir]
  (-> (build-vcs/git-changes?-cmd)
      (build-commands/blocking-cmd app-dir)
      build-vcs/git-changes?-analyze
      not))

;;push branch
(defn- replace-branch-files
  "Replaces files from target-branch with files from files-dir. Returns directory in which it resides"
  [files-dir repo-address target-branch]
  (let [target-git-dir (target-branch-git-dir repo-address target-branch)
        dir-with-replaced-files (build-files/create-temp-dir)]
    (build-headers-files/copy-files-or-dir [files-dir] dir-with-replaced-files)
    (replace-repo-git-dir target-git-dir dir-with-replaced-files)
    dir-with-replaced-files))

(defn push-local-dir-to-repo
  "Commit and push `target-branch` with files from `source-dir`.
  Params:
  * `source-dir` local directory where the sources are stored, before being pushed to the remote repo
  * `repo-address` the address of the repo
  * `commit-msg` message that will end in pushed commit
  * `version` string with tag version
  * `tag` (optional) map containg `id` with tag and optional `msg` with corresponding message
  * `force?` (optional default false) if true, will force the changes to be pushed as top commit
  * `target-branch` (optional default current-branch) where to push"
  ([source-dir repo-address force? target-branch commit-msg]
   (let [dir-to-push (replace-branch-files source-dir repo-address target-branch)]
     (commit-and-push dir-to-push commit-msg (current-branch dir-to-push) force?))))

(defn push-current-branch
  "Pushes `app` current changes to it's repository. The changes are pushed to the current branch of user running the function. It is forbidden to push to base-branch of application."
  [app-dir app-name repo main-branch current-branch force? message]
  (cond
    (not-every? some? [app-dir repo main-branch current-branch])
    (normalln app-name " is skipped due to missing value")
    (= current-branch main-branch)
    (normalln app-name
              (format "`%s` is skipped as branch name `%s` is the same as main branch of the app"
                      app-name
                      current-branch))
    :else (if (or force?
                  (build-cli-input/yes-question
                   (format "Are you sure you want push changes from `%s` to branch `%s`?"
                           app-name
                           current-branch)))
            (push-local-dir-to-repo app-dir
                                    repo
                                    force?
                                    current-branch
                                    (when (and message (string? message) (not (str/blank? message)))
                                      message))
            true)))

;; this for deployment
;; (defn push-base-branch
;;   "Pushes app to `base-branch`"
;;   [app-name app-dir repo base-branch version message]
;;   (let [github-new-changes-link (str "https://github.com/" (first (re-find #"(?<=:)(.*)(?=.git)"
;;                                                                            repo))
;;                                      "/tree/" base-branch)]
;;     (build-log/info-format "Pushing app: `%s` to branch: `%s`. \nChanges can be seen here `%s`"
;;                            app-name
;;                            base-branch
;;                            github-new-changes-link)
;;     (build-cfg-mgt/push-local-dir-to-repo {:source-dir app-dir
;;                                            :repo-address repo
;;                                            :tag {:id version
;;                                                  :msg message}
;;                                            :commit-msg message
;;                                            :force? true
;;                                            :target-branch base-branch})))


(defn push-local-branch
  []
  (let [{:keys [repo env]} publication
        main-branch (get-in env [:production :push-branch])]
    (if (push-current-branch app-dir app-name repo main-branch message-opt force)
      build-exit-codes/ok
      build-exit-codes/catch-all)))

(defn run-monorepo
  []
  (let [monorepo-project-map (-> (build-project-map/create-project-map "")
                                 build-project-map/add-project-config
                                 (build-apps/add-monorepo-subprojects :default)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config))
        env (get-in cli-opts [:options :env])
        force (get-in cli-opts [:options :force])
        message (get-in cli-opts [:options :message])
        base-branch (get-in monorepo-project-map
                            [:project-config-filedesc :edn :publication :base-branch])
        app-dir (get-in monorepo-project-map [:app-dir])
        current-branch (build-headers-vcs/current-branch app-dir)
        subapps (->> monorepo-project-map
                     :subprojects)]
    ;; Monorepo level
    (if-not (pull-base-branch app-dir base-branch current-branch)
      (do
        (h1-error!
         "Current branch is not up-to-date with base, first pull changes from monorepo main branch")
        1)
      (do (h1-valid! "Current branch is up-to-date with base.")
          (if-not (clean-state? app-dir)
            (do (h1-error! "Git status is not empty. First commmit your changes.") 1)
            (let [push-res
                  (map
                   (fn [{:keys [app-dir app-name]
                         :as app}]
                     (let [app-base-branch
                           (get-in app [:project-config-filedesc :edn :publication :base-branch])
                           repo (get-in app [:project-config-filedesc :edn :publication :repo-url])]
                       (push-current-branch app-dir
                                            app-name
                                            repo
                                            app-base-branch
                                            current-branch
                                            force
                                            message)))
                   subapps)]))))
    ;;Per app
    ;; (map push-local-branch subapps)
    ;; and deploy per app
  ))
