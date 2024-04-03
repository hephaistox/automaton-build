(ns automaton-build.tasks.git-push-local-branch
  (:require
   [automaton-build.app.git-push-local-code :as build-app-git-push-local-code]
   [automaton-build.os.exit-codes           :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Push to app repository, current git branch, changes from `app-dir`"
  [_task-map
   {:keys [app-dir app-name publication message-opt force]
    :as _app-data}]
  (let [{:keys [repo env]} publication
        main-branch (get-in env [:production :push-branch])]
    (if (build-app-git-push-local-code/push-current-branch app-dir
                                                           app-name
                                                           repo
                                                           main-branch
                                                           message-opt
                                                           force)
      build-exit-codes/ok
      build-exit-codes/catch-all)))
