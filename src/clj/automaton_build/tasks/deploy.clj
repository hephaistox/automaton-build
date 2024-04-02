(ns automaton-build.tasks.deploy
  "Compile, deploy to gha, push to base branch and publish jar."
  (:require
   [automaton-build.app.versioning              :as build-app-versioning]
   [automaton-build.cicd.cfg-mgt                :as build-cfg-mgt]
   [automaton-build.cicd.version                :as build-version]
   [automaton-build.log                         :as build-log]
   [automaton-build.os.exit-codes               :as build-exit-codes]
   [automaton-build.os.files                    :as build-files]
   [automaton-build.os.terminal-msg             :as build-terminal-msg]
   [automaton-build.tasks.build-jar             :as build-tasks-build-jar]
   [automaton-build.tasks.gha-container-publish
    :as build-tasks-gha-container-publish]
   [automaton-build.tasks.git-push-base-branch
    :as build-tasks-git-push-base-branch]
   [automaton-build.tasks.publish-jar           :as build-tasks-publish-jar]
   [automaton-build.utils.keyword               :as build-utils-keyword]))

(defn- base-branch-push-disallowed
  [publication]
  (let [current-branch (build-cfg-mgt/current-branch ".")
        base-branches (map #(get-in publication [:env % :push-branch])
                           (keys (:env publication)))]
    (if (some #(= current-branch %) base-branches)
      (do
        (build-terminal-msg/println-msg
         "Can't push to base production branch, please switch to your development branch for testing.")
        true)
      false)))

(defn- state-should-be-clean
  [dir]
  (if (build-cfg-mgt/git-changes? dir)
    (do (build-terminal-msg/println-msg
         "Can't continue with deployment when changes are in progress")
        true)
    false))

(defn deployment*
  [task-map
   {:keys [app-name]
    :as app}]
  (if (build-tasks-git-push-base-branch/exec task-map app)
    (if (= build-exit-codes/ok (build-tasks-publish-jar/exec task-map app))
      (do (build-log/info-format "Deploy of %s successful" app-name)
          build-exit-codes/ok)
      (do (build-log/warn-format "Deploy step of %s failed" app-name)
          build-exit-codes/catch-all))
    (do (build-log/warn-format "Pushing %s failed - deploy aborted" app-name)
        build-exit-codes/catch-all)))

(defn- ensure-no-cache
  "Ensures app is deployed in isolation, without any cache.
   So it clones current branch to temporary directory"
  [{:keys [app-name publication]
    :as app}]
  (let [new-changes-branch (build-cfg-mgt/current-branch ".")
        {:keys [repo]} publication
        tmp-dir (build-files/create-temp-dir)
        tmp-dir-app (build-files/create-dir-path tmp-dir app-name)
        app (assoc app :app-dir tmp-dir-app)]
    (build-cfg-mgt/clone-repo-branch tmp-dir app-name repo new-changes-branch)
    app))

(defn deploy?
  "Automatic check if app should be deployed"
  [publication environment app-dir app-name]
  (if-let [repo (:repo publication)]
    (let [environment (-> environment
                          build-utils-keyword/trim-colon
                          build-utils-keyword/keywordize)
          target-branch (get-in publication [:env environment :push-branch])]
      (if (build-app-versioning/version-changed? app-dir
                                                 app-name
                                                 repo
                                                 target-branch)
        true
        false))
    (do (build-log/debug-format "`%s` does not have a git repo so it's skipped"
                                app-name)
        false)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Deploys app in isolation to ensure no cache which requires that your state should be clean and current branch is not a base branch

   The process of deployment is:
   1. Compile jar
   2. Publish new GHA docker image (if applies)
   3. Push changes to app target branch for that environment
   4. Deploy app

  It's done currently not as a workflow, only because workflow can't be used in another workflow and we have a logic we want to preserve here"
  [task-map
   {:keys [app-name publication app-dir gha force environment]
    :as app}]
  (if (or (base-branch-push-disallowed publication)
          (state-should-be-clean app-dir)
          (and (not force)
               (not (deploy? publication environment app-dir app-name))))
    (do (build-log/info-format
         "Deployment skipped for `%s` as initial conditions are not met"
         app-name)
        build-exit-codes/ok)
    (let [app (ensure-no-cache app)]
      (build-log/info-format "Deployment process started for `%s`" app-name)
      (if (= build-exit-codes/ok (build-tasks-build-jar/exec task-map app))
        (if gha
          (if (= build-exit-codes/ok
                 (build-tasks-gha-container-publish/exec
                  task-map
                  (assoc app
                         :tag
                         (build-version/current-version (:app-dir app)))))
            (deployment* task-map app)
            (do (build-log/warn-format
                 "GHA deploy of %s failed - deploy aborted"
                 app-name)
                build-exit-codes/catch-all))
          (do (build-log/debug-format
               "Gha missing for `%s`, gha-container-publish is skipped"
               app-name)
              (deployment* task-map app)))
        (do (build-log/warn-format "Compilation of %s failed - deploy aborted"
                                   app-name)
            build-exit-codes/catch-all)))))
