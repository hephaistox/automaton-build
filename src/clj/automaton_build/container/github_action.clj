(ns automaton-build.container.github-action
  "Manage the github action containers"
  (:require
   [automaton-build.adapters.container :as adapter-container]
   [automaton-core.adapters.deps-edn :as deps-edn]
   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]
   [automaton-core.env-setup :as env-setup]
   [automaton-build.app.code-publication :as app-code-pub]))

(def image-name
  (files/create-file-path (get-in env-setup/env-setup
                                  [:container-repo :gha :repo-name])))

(def image-src-dir
  (files/create-file-path (get-in env-setup/env-setup
                                  [:container-repo :gha :source-dir])))

(def assembly-subdir
  (get-in env-setup/env-setup [:container-repo :assembly-subdir]))

(def remote-repo-account
  (get-in env-setup/env-setup [:container-repo :account]))

(defn create-gha-image-name
  "Create the name of the image to run the app
  Params:
  * `app-name` is the name of the application to build"
  [app-name]
  (str image-name "-" app-name))

(defn build
  "Build the github action image
  Params:
  * `app-name` is the name of the application container to build
  * `app-dir` where the app to build cc for is stored
  * `publish?` (Optional, default true) if true publish the image
  Return the name of the image built"
  ([app-name app-dir publish?]
   (let [image-name (create-gha-image-name app-name)
         assembled-container-dir (files/create-dir-path (app-code-pub/assembly-app-dir app-name)
                                                        assembly-subdir
                                                        image-name)
         app-files-to-copy-in-cc-container (map (partial files/create-file-path app-dir)
                                                [deps-edn/deps-edn "package.json" "shadow-cljs.edn"])]
     (log/debug "Create githubaction container image for cust-app `" app-name "` in directory `" assembled-container-dir "`")
     (adapter-container/build-and-push-image image-name
                                             remote-repo-account
                                             image-src-dir
                                             assembled-container-dir
                                             app-files-to-copy-in-cc-container
                                             publish?)
     image-name))
  ([app-name app-dir]
   (build app-name app-dir true)))

(defn connect
  "Connect to the git hub action image
  Params:
  * `app-name` use the name of the application, used to build container image name
  * `app-dir` where the app to build cc for is stored
  * `local-dir` local directory to connect"
  [app-name app-dir local-dir]
  (let [image-name (build app-name app-dir false)]
    (adapter-container/container-interactive image-name
                                             local-dir)))
