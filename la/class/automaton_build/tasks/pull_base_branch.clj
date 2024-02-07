(ns automaton-build.tasks.pull-base-branch
  (:require
   [automaton-build.cicd.cfg-mgt :as build-cfg-mgt]
   [automaton-build.log :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]))

(defn- current-branch-not-base
  [current-branch base-branch]
  (not (= current-branch base-branch)))

(defn- pull-base-branch
  "Pull base branch, cannot be executed on base branch"
  [app-dir base-branch]
  (let [current-branch (build-cfg-mgt/current-branch app-dir)]
    (when (current-branch-not-base current-branch base-branch)
      (let [res (build-cfg-mgt/pull-changes base-branch current-branch app-dir)]
        (and (zero? (ffirst res)) (zero? (first (second res))))))))


#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map {:keys [publication app-dir]}]
  (build-log/info "Pull base branch started")
  (let [base-branch (get-in publication [:env :production :push-branch])]
    (if (pull-base-branch app-dir base-branch)
      build-exit-codes/ok
      build-exit-codes/catch-all)))
