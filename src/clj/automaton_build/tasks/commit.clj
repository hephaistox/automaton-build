(ns automaton-build.tasks.commit
  (:require
   [automaton-build.cicd.cfg-mgt    :as build-cfg-mgt]
   [automaton-build.os.exit-codes   :as build-exit-codes]
   [automaton-build.os.terminal-msg :as build-terminal-msg]))

(defn- base-branch-push-disallowed
  [current-branch main-branch]
  (if (= current-branch main-branch)
    (do (build-terminal-msg/println-msg
         "Can't push to main production branch, please switch to development branch to commit.")
        true)
    false))

(defn- commit
  [main-branch app-dir message-opt]
  (let [current-branch (build-cfg-mgt/current-branch app-dir)]
    (if-not (base-branch-push-disallowed current-branch main-branch)
      (ffirst (build-cfg-mgt/commit-and-push app-dir message-opt current-branch))
      false)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Commit and push. Is not allowed for production base-branch."
  [_task-map {:keys [message-opt publication app-dir]}]
  (if (nil? message-opt)
    (do (build-terminal-msg/println-msg "Committing skipped, due to missing -m option.")
        build-exit-codes/ok)
    (let [res (commit (get-in publication [:env :production :push-branch]) app-dir message-opt)]
      (cond
        (nil? res) build-exit-codes/ok
        (number? res) res
        :else build-exit-codes/catch-all))))
