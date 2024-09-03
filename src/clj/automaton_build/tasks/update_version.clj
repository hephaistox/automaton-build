(ns automaton-build.tasks.update-version
  "Updates version "
  (:require
   [automaton-build.app.versioning :as build-app-versioning]
   [automaton-build.cicd.version   :as build-version]
   [automaton-build.log            :as build-log]
   [automaton-build.os.exit-codes  :as build-exit-codes]))

(defn update-version
  [app-dir app-name target-env]
  (if-let [version (build-app-versioning/generate-new-app-version target-env app-dir app-name)]
    (if-let [_save-version (build-version/save-version app-dir version)]
      (do (build-log/info-format "New version %s for %s" version app-name) true)
      (do (build-log/warn "Abort, as saving new version failed") false))
    (build-log/warn "Abort, as to continue user permission is needed")))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Update version of the project"
  [_task-map
   {:keys [app-dir app-name environment]
    :as _app}]
  (if (true? (update-version app-dir app-name environment))
    build-exit-codes/ok
    build-exit-codes/cannot-execute))
