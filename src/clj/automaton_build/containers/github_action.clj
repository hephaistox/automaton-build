(ns automaton-build.containers.github-action
  "Manage the github action containers"
  (:require
   [automaton-build.app.deps-edn :as build-deps-edn]
   [automaton-build.containers :as build-containers]
   [automaton-build.containers.local-engine :as build-local-engine]
   [automaton-build.log :as build-log]
   [automaton-build.os.files :as build-files]))

(defrecord GithubAction [app-name container-dir app-dir remote-repo-account tag]
  build-containers/Container
    (container-tagged-name [_] (format "gha-%s:%s" app-name tag))
    (container-name [_] (format "gha-%s:" app-name))
    (build [this publish?]
      (let [app-files-to-copy-in-cc-container
            (map (partial build-files/create-file-path app-dir)
                 [build-deps-edn/deps-edn "package.json"])
            image-name (build-containers/container-tagged-name this)]
        (build-log/debug-format
         "Create github-action container image `%s` for cust-app `%s`"
         image-name
         app-name)
        (build-local-engine/build-and-push-image
         image-name
         remote-repo-account
         container-dir
         (build-files/create-temp-dir image-name)
         app-files-to-copy-in-cc-container
         publish?)))
    (connect [this]
      (if (build-containers/build this false)
        (build-local-engine/container-interactive
         (build-containers/container-tagged-name this)
         app-dir)
        (build-log/warn
         "Connection to the container is skipped as build has failed"))))

(defn make-github-action
  "Create a manager for github action container
  * `app-name` the name of the app
  * `container-dir` where the container is stored
  * `image-src` where the image of the container is stored
  * `remote-repo-account` account to connect the repo to
  * `tag` of the build"
  [app-name container-dir app-dir remote-repo-account tag]
  (->GithubAction app-name container-dir app-dir remote-repo-account tag))
