(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.cicd.cfg-mgt                :as build-cfg-mgt]
   [automaton-build.cicd.deployment             :as build-deployment]
   [automaton-build.cicd.version                :as build-version]
   [automaton-build.log                         :as build-log]
   [automaton-build.os.exit-codes               :as build-exit-codes]
   [automaton-build.os.files                    :as build-files]
   [automaton-build.os.terminal-msg             :as build-terminal-msg]
   [automaton-build.tasks.build-jar             :as build-tasks-build-jar]
   [automaton-build.tasks.gha-container-publish
    :as build-tasks-gha-container-publish]
   [automaton-build.tasks.publish-jar           :as build-tasks-publish-jar]
   [automaton-build.utils.keyword               :as build-utils-keyword]))

(defn prepare-tmp-dir-app!
  [composite-tmp-dir app-name repo new-changes-branch target-dir local-dir]
  (build-cfg-mgt/clone-repo-branch composite-tmp-dir
                                   app-name
                                   repo
                                   new-changes-branch)
  (build-files/copy-files-or-dir [local-dir] target-dir)
  (build-cfg-mgt/clean target-dir))

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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Deploys app in isolation to ensure no cache.
   All files for deployment should be prepared before running this task (e.g. version update, deps update, pom.xml...)

   To start the deployment, your current branch state should be clean and deployment itself started from not a base-branch.

   The process of deployment is:
   1. Compile jar
   2. Publish new GHA docker image
   3. Push changes to app target branch for that environment
   4. Deploy app

  It's done currently not as a workflow, only because workflow can't be used in another workflow and we have a logic we want to preserve here"
  [task-map
   {:keys [app-name publication environment app-dir]
    :as app}]
  (if (or (base-branch-push-disallowed publication)
          (state-should-be-clean app-dir))
    build-exit-codes/ok
    (let [new-changes-branch (build-cfg-mgt/current-branch ".")
          composite-tmp-dir (build-files/create-temp-dir)
          {:keys [repo]} publication
          tmp-dir-app (build-files/create-dir-path composite-tmp-dir app-name)
          app (assoc app :app-dir tmp-dir-app)]
      (prepare-tmp-dir-app! composite-tmp-dir
                            app-name
                            repo
                            new-changes-branch
                            tmp-dir-app
                            app-dir)
      (let [environment (-> environment
                            build-utils-keyword/trim-colon
                            build-utils-keyword/keywordize)
            {:keys [deploy-to repo env]} publication
            branch (get-in env [environment :push-branch])
            version (build-version/current-version (:app-dir app))]
        (build-log/info-format "Deployment process started to `%s`" deploy-to)
        (build-log/trace "App deploy process has started")
        (if (= build-exit-codes/ok (build-tasks-build-jar/exec task-map app))
          (if (= build-exit-codes/ok
                 (build-tasks-gha-container-publish/exec
                  task-map
                  (assoc app
                         :tag
                         (build-version/current-version (:app-dir app)))))
            (if (build-deployment/push-app-base app-name
                                                (:app-dir app)
                                                repo
                                                branch
                                                version
                                                version)
              (if (= build-exit-codes/ok
                     (build-tasks-publish-jar/exec task-map app))
                (do (build-log/info-format "Deploy of %s successful" app-name)
                    build-exit-codes/ok)
                (do (build-log/warn-format "Deploy step of %s failed" app-name)
                    build-exit-codes/catch-all))
              (do (build-log/warn-format "Pushing %s failed - deploy aborted"
                                         app-name)
                  build-exit-codes/catch-all))
            (do (build-log/warn-format
                 "GHA deploy of %s failed - deploy aborted"
                 app-name)
                build-exit-codes/catch-all))
          (do (build-log/warn-format "Compilation of %s failed - deploy aborted"
                                     app-name)
              build-exit-codes/catch-all))))))
