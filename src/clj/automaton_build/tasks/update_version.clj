(ns automaton-build.tasks.update-version
  (:require
   [automaton-build.cicd.version  :as build-version]
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn update-version
  [app-dir app-name target-env]
  (let [current-version (build-version/current-version app-dir)]
    (build-log/info-format "For `%s` current version is `%s`"
                           app-name
                           current-version)
    (if-let [version
             (if (= :production (keyword target-env))
               (build-version/generate-production-version current-version
                                                          app-name)
               (build-version/generate-test-env-version current-version
                                                        app-name
                                                        (name target-env)))]
      (if-let [_save-version (build-version/save-version app-dir version)]
        (do (build-log/info-format "New version %s" version) true)
        (do (build-log/warn "Abort, as saving new version failed") false))
      (build-log/warn "Abort, as to continue user permission is needed"))))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Update version of the project"
  [_task-map
   {:keys [app-dir app-name environment]
    :as _app}]
  (if (true? (update-version app-dir app-name environment))
    build-exit-codes/ok
    build-exit-codes/cannot-execute))
