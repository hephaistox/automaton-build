(ns automaton-build.cicd.deployment.docker
  (:require
   [automaton-build.cicd.cfg-mgt :as build-cfg-mgt]
   [automaton-build.cicd.server :as build-cicd-server]
   [automaton-build.containers :as build-containers]))

(defn publish-container
  [container container-dir tag workflows repo-url repo-branch]
  (let [container-name (build-containers/container-name container)]
    (build-cicd-server/show-tag-in-workflows workflows container-name)
    (build-cfg-mgt/clone-repo-branch container-dir repo-url repo-branch)
    (and container
         (build-containers/build container true)
         (build-cicd-server/update-workflows workflows tag container-name))))
