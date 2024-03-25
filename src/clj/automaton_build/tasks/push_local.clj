(ns automaton-build.tasks.push-local
  (:require
   [automaton-build.cicd.deployment :as build-deployment]
   [automaton-build.os.exit-codes   :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Push local repository to current branch"
  [_task-map
   {:keys [app-dir app-name publication message-opt force?]
    :as _app-data}]
  (let [{:keys [repo env]} publication
        main-branch (get-in env [:production :push-branch])]
    (if (build-deployment/push-app-local app-dir
                                         app-name
                                         repo
                                         main-branch
                                         message-opt
                                         force?)
      build-exit-codes/ok
      build-exit-codes/catch-all)))
