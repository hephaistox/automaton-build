(ns automaton-build.headers.commit
  "In a workflow, commit."
  (:require
   [automaton-build.code.vcs    :as build-vcs]
   [automaton-build.headers.vcs :as build-headers-vcs]
   [automaton-build.os.cmds     :as build-commands]))

;; (defn commit
;;   "Commit all local changes in the directory `dir` - normally the root directory of a `git` repo, but it works inside in the repo also.

;;   All unstaged changes are staged and commited with message `commit-message`.

;;   Returns `true` if the commit is ok."
;;   [dir commit-message verbose]
;;   (h1 "Commit")
;;   (let [s (build-writter)
;;         commit-status
;;         (binding [*out* s]
;;           (let [commit-res (-> commit-message
;;                                build-vcs/commit-chain-cmd
;;                                (force-dirs dir)
;;                                (chain-cmds "Commit has failed." verbose)
;;                                build-commands/first-failing
;;                                build-vcs/commit-analyze)
;;                 {:keys [nothing-to-commit exit]} commit-res]
;;             (cond
;;               nothing-to-commit (normalln "Skipped as no changes found.")
;;               (not (zero? exit)) (errorln "Unexpected error.")
;;               :else (normalln "Commit" (build-headers-vcs/latest-commit-sha) "has been created"))
;;             (success commit-res)))]
;;     (if commit-status (h1-valid "Commit ok") (h1-error "Commit has failed."))
;;     (print-writter s)
;;     (normalln "Commit message: " commit-message)
;;     commit-status))
