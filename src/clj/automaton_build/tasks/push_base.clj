(ns automaton-build.tasks.push-base
  (:require
   [automaton-build.cicd.deployment :as build-deployment]
   [automaton-build.os.exit-codes   :as build-exit-codes]
   [automaton-build.utils.keyword   :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Push the current repository from current branch"
  [_task-map
   {:keys [app-dir app-name publication message force? environment]
    :as _app-data}]
  (let [environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        {:keys [repo env]} publication
        main-branch (get-in env [environment :push-branch])]
    (if (build-deployment/push-app-base app-name
                                        app-dir
                                        repo
                                        main-branch
                                        message
                                        force?)
      build-exit-codes/ok
      build-exit-codes/catch-all)))
