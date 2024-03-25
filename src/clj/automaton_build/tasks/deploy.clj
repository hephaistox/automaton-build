(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.cicd.deployment             :as build-deployment]
   [automaton-build.cicd.version                :as build-version]
   [automaton-build.log                         :as build-log]
   [automaton-build.os.exit-codes               :as build-exit-codes]
   [automaton-build.tasks.build-jar             :as build-tasks-build-jar]
   [automaton-build.tasks.gha-container-publish
    :as build-tasks-gha-container-publish]
   [automaton-build.tasks.publish-jar           :as build-tasks-publish-jar]
   [automaton-build.utils.keyword               :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Deploys app.
   The process of deployment is:
   1. Compile jar
   2. Publish new GHA docker image
   3. Push changes to app target branch for that environment
   4. Deploy app"
  [task-map
   {:keys [app-name publication environment app-dir]
    :as app}]
  (let [environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        {:keys [deploy-to repo env]} publication
        branch (get-in env [environment :push-branch])
        version (build-version/current-version app-dir)]
    (build-log/info-format "Deployment process started to `%s`" deploy-to)
    (build-log/trace "App deploy process has started")
    (if (= build-exit-codes/ok (build-tasks-build-jar/exec task-map app))
      (if (= build-exit-codes/ok
             (build-tasks-gha-container-publish/exec
              task-map
              (assoc app :tag (build-version/current-version app-dir))))
        (if (build-deployment/push-app-base app-name
                                            app-dir
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
        (do (build-log/warn-format "GHA deploy of %s failed - deploy aborted"
                                   app-name)
            build-exit-codes/catch-all))
      (do (build-log/warn-format "Compilation of %s failed - deploy aborted"
                                 app-name)
          build-exit-codes/catch-all))))
