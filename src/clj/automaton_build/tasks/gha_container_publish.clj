(ns automaton-build.tasks.gha-container-publish
  (:require
   [automaton-build.cicd.deployment.docker :as build-deployment-docker]
   [automaton-build.log                    :as build-log]
   [automaton-build.os.exit-codes          :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Build the container, publish the local code
  The gha container is adapted for each application, so this build is tagged with the name of the app and its version.
  The deps files are copied in the docker to preload all deps (for instance all `deps.edn`)"
  [_task-map {:keys [app-dir app-name tag account gha]}]
  (build-log/info "Build and publish github container")
  (let [{:keys [repo-url repo-branch workflows]} gha]
    (if gha
      (if (build-deployment-docker/publish-test-docker-image repo-url
                                                             repo-branch
                                                             workflows
                                                             app-dir
                                                             app-name
                                                             tag
                                                             account)
        build-exit-codes/ok
        build-exit-codes/catch-all)
      (do (build-log/debug-format
           "Gha missing for `%s`, gha-container-publish is skipped"
           app-name)
          build-exit-codes/ok))))
