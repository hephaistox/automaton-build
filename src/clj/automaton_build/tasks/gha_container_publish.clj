(ns automaton-build.tasks.gha-container-publish
  (:require
   [automaton-build.cicd.cfg-mgt :as build-cfg-mgt]
   [automaton-build.cicd.server :as build-cicd-server]
   [automaton-build.containers :as build-containers]
   [automaton-build.containers.github-action :as build-github-action]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files :as build-files]))

(defn- push-gha-from-local*
  [container-url container-dir container tag gha-workflows repo-branch]
  (build-cfg-mgt/clone-repo-branch container-dir container-url repo-branch)
  (let [container-name (build-containers/container-name container)]
    (if-not (and container
                 (build-containers/build container true)
                 (build-cicd-server/update-workflows gha-workflows
                                                     tag
                                                     container-name))
      build-exit-codes/catch-all
      build-exit-codes/ok)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Build the container, publish the local code
  The gha container is adapted for each application, so this build is tagged with the name of the app and its version.
  The deps files are copied in the docker to preload all deps (for instance all `deps.edn`)"
  [_task-map {:keys [app-dir app-name tag account gha]}]
  (build-log/info "Build and publish github container")
  (let [{:keys [repo-url repo-branch workflows]} gha
        container-dir (build-files/create-temp-dir "gha-container")
        container (build-github-action/make-github-action app-name
                                                          container-dir
                                                          app-dir
                                                          account
                                                          tag)
        container-name (build-containers/container-name container)]
    (build-cicd-server/show-tag-in-workflows workflows container-name)
    (push-gha-from-local* repo-url
                          container-dir
                          container
                          tag
                          workflows
                          repo-branch)))

