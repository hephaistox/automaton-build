(ns automaton-build.tasks.update-gha-workflow-file
  "Updates GitHub Action workflow file tag version"
  (:require
   [automaton-build.cicd.cfg-mgt                 :as build-cfg-mgt]
   [automaton-build.cicd.deployment.workflow-yml :as build-workflow-yml]
   [automaton-build.cicd.version                 :as build-version]
   [automaton-build.containers                   :as build-containers]
   [automaton-build.containers.github-action     :as build-github-action]
   [automaton-build.log                          :as build-log]
   [automaton-build.os.exit-codes                :as build-exit-codes]
   [automaton-build.os.files                     :as build-files]))

(defn update-gha-workflow-file?
  "Gha workflow file is to be updated if the version.edn file is modified"
  [app-dir]
  (build-cfg-mgt/file-modified? (build-version/version-file app-dir)))

(defn exec
  [_task-map
   {:keys [app-dir app-name gha account force]
    :as _app}]
  (build-log/info-format "Starting gha workflow files version update for %s"
                         app-name)
  (if (or force (update-gha-workflow-file? app-dir))
    (let [current-version (build-version/current-version app-dir)
          {:keys [workflows]} gha
          container (build-github-action/make-github-action app-name
                                                            ""
                                                            app-dir
                                                            account
                                                            current-version)
          workflow-paths (map #(build-files/create-dir-path app-dir %)
                              workflows)
          container-name (build-containers/container-name container)]
      (if (->> (for [filename workflow-paths]
                 (build-workflow-yml/spit-workflow filename
                                                   container-name
                                                   current-version))
               (every? true?))
        build-exit-codes/ok
        build-exit-codes/catch-all))
    build-exit-codes/ok))
