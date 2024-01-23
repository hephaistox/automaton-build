(ns automaton-build.cicd.deployment
  (:require
   [automaton-build.cicd.clever-cloud :as build-clever-cloud]
   [automaton-build.cicd.deployment.docker :as build-deploy-docker]
   [automaton-build.cicd.deployment.jar :as build-deploy-jar]
   [automaton-build.configuration :as build-conf]
   [automaton-build.containers.github-action :as build-github-action]
   [automaton-build.os.files :as build-files]))

(defn publish-library
  "Publish jar to clojars"
  [jar-path pom-path]
  (build-deploy-jar/deploy
   jar-path
   (build-files/absolutize pom-path)
   {"clojars" {:url "https://clojars.org/repo"
               :username (build-conf/read-param [:clojars-username])
               :password (build-conf/read-param [:clojars-password])}})
  true)

(defn publish-app
  "Publish uber-jar to Clever Cloud. [clever docs](https://developers.clever-cloud.com/doc/cli/)"
  ([repo-uri app-dir]
   (let [clever-dir (build-files/absolutize
                     (build-files/create-dir-path app-dir ".clever"))
         target-dir (build-files/absolutize
                     (build-files/create-dir-path app-dir "target"))
         clever-repo-dir
         (build-clever-cloud/clone-repo clever-dir repo-uri "repo")]
     (build-files/copy-files-or-dir [target-dir]
                                    (build-files/create-dir-path clever-repo-dir
                                                                 "target"))
     (build-clever-cloud/deploy clever-repo-dir)))
  ([repo-uri] (publish-app repo-uri ".")))

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
        container (build-github-action/make-github-action app-name
                                                          container-dir
                                                          app-dir
                                                          account
                                                          tag)]
    (build-deploy-docker/publish-container container
                                           container-dir
                                           tag
                                           workflows
                                           repo-url
                                           repo-branch)))
