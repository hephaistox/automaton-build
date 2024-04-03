(ns automaton-build.tasks.gha-lconnect
  (:require
   [automaton-build.cicd.cfg-mgt             :as build-cfg-mgt]
   [automaton-build.containers               :as build-containers]
   [automaton-build.containers.github-action :as build-github-action]
   [automaton-build.log                      :as build-log]
   [automaton-build.os.exit-codes            :as build-exit-codes]
   [automaton-build.os.files                 :as build-files]))

(defn- gha-lconnect*
  [tmp-dir repo-url repo-branch app-name container-repo-account tag]
  (if-let [exit-code
           (and (build-cfg-mgt/clone-repo-branch tmp-dir repo-url repo-branch)
                (some-> (build-github-action/make-github-action
                         app-name
                         tmp-dir
                         ""
                         container-repo-account
                         tag)
                        build-containers/connect
                        ffirst))]
    (if (number? exit-code) exit-code build-exit-codes/catch-all)
    (do (build-log/fatal "Error during gha connection")
        build-exit-codes/catch-all)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Task to locally connect to github action"
  [_task-map
   {:keys [account app-name gha tag]
    :as _app}]
  (build-log/info "Run and connect to github container locally")
  (let [tmp-dir (build-files/create-temp-dir "gha_image")]
    (gha-lconnect* tmp-dir
                   (:repo-url gha)
                   (:repo-branch gha)
                   app-name
                   account
                   tag)))
