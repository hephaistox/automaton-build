(ns automaton-build.tasks.git-push-base-branch
  (:require
   [automaton-build.app.git-push-local-code :as build-app-git-push-local-code]
   [automaton-build.cicd.version            :as build-version]
   [automaton-build.os.exit-codes           :as build-exit-codes]
   [automaton-build.utils.keyword           :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Push local repository to base branch"
  [_task-map
   {:keys [app-dir app-name publication force? environment]
    :as _app-data}]
  (let [environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        {:keys [repo env]} publication
        main-branch (get-in env [environment :push-branch])]
    (if (build-app-git-push-local-code/push-base-branch
         app-name
         app-dir
         repo
         main-branch
         (build-version/current-version app-dir)
         force?)
      build-exit-codes/ok
      build-exit-codes/catch-all)))
