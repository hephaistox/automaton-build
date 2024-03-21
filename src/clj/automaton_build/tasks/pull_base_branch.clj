(ns automaton-build.tasks.pull-base-branch
  (:require
   [automaton-build.cicd.cfg-mgt  :as build-cfg-mgt]
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn- current-branch-not-base
  [current-branch base-branch]
  (not (= current-branch base-branch)))

(defn- pull-base-branch
  "Pull base branch, cannot be executed on base branch"
  [app-dir base-branch]
  (let [current-branch (build-cfg-mgt/current-branch app-dir)]
    (when (current-branch-not-base current-branch base-branch)
      (let [res (build-cfg-mgt/pull-changes base-branch app-dir)]
        (build-log/trace-format
         "Merge of branch `%s` to branch `%s`, has resulted with: `%s`"
         base-branch
         current-branch
         res)
        (and (zero? (ffirst res)) (zero? (first (second res))))))))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [publication app-dir]}]
  (let [base-branch (get-in publication [:env :production :push-branch])]
    (if (pull-base-branch app-dir base-branch)
      (do (build-log/debug "Current branch is up-to-date with base.")
          build-exit-codes/ok)
      (do (build-log/warn "Current branch is not up-to-date with base.")
          build-exit-codes/catch-all))))
