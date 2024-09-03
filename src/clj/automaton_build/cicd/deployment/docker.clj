(ns automaton-build.cicd.deployment.docker
  (:require
   [automaton-build.cicd.cfg-mgt                 :as build-cfg-mgt]
   [automaton-build.cicd.deployment.workflow-yml :as build-workflow-yml]
   [automaton-build.containers                   :as build-containers]
   [automaton-build.containers.github-action     :as build-github-action]
   [automaton-build.os.files                     :as build-files]))

(defn publish-container
  [container container-dir workflows repo-url repo-branch]
  (let [container-name (build-containers/container-name container)]
    (build-workflow-yml/show-tag-in-workflows workflows container-name)
    (build-cfg-mgt/clone-repo-branch container-dir repo-url repo-branch)
    (and container (build-containers/build container true))))

(defn publish-test-docker-image
  "Publish gha image to docker.
   Params:
   * `repo-url` repository of an app for gha to publish
   * `repo-branch` branch to use
   * `workflows` path from the repo to commit_validation.yml
   * `app-dir`
   * `app-name`
   * `tag` version
   * `account` docker organization name"
  [repo-url repo-branch workflows app-dir app-name tag account]
  (let [container-dir (build-files/create-temp-dir "gha-container")
        container
        (build-github-action/make-github-action app-name container-dir app-dir account tag)
        workflow-paths (map #(build-files/create-dir-path app-dir %) workflows)]
    (publish-container container container-dir workflow-paths repo-url repo-branch)))
