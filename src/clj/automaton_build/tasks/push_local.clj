(ns automaton-build.tasks.push-local
  (:require
   [automaton-build.cicd.cfg-mgt :as build-cfg-mgt]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Push the current repository from current branch"
  [_task-map
   {:keys [app-dir publication message force?]
    :as _app-data}]
  (let [{:keys [repo branch]} publication]
    (build-log/debug-format "Push local `%s` " message)
    (if (true? (build-cfg-mgt/push-local-dir-to-repo {:source-dir app-dir
                                                      :repo-address repo
                                                      :base-branch-name branch
                                                      :commit-msg message
                                                      :force? force?}))
      build-exit-codes/ok
      build-exit-codes/catch-all)))
