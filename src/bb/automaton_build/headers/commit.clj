(ns automaton-build.tasks.impl.commit
  "In a workflow, commit."
  (:require
   [automaton-build.code.vcs                :as build-vcs]
   [automaton-build.echo.headers            :refer [build-writter
                                                    errorln
                                                    h1
                                                    h1-error
                                                    h1-valid
                                                    h2-error!
                                                    h2-valid!
                                                    normalln
                                                    print-writter
                                                    print-writter]]
   [automaton-build.os.cmds                 :as build-commands]
   [automaton-build.tasks.impl.headers.cmds :refer [chain-cmds force-dirs success]]
   [automaton-build.tasks.impl.headers.vcs  :as build-headers-vcs]))

(defn commit
  "Commit all local changes in the directory `dir` - normally the root directory of a `git` repo, but it works inside in the repo also.

  All unstaged changes are staged and commited with message `commit-message`.

  Returns `true` if the commit is ok."
  [dir commit-message verbose]
  (h1 "Commit")
  (let [s (build-writter)
        commit-status
        (binding [*out* s]
          (let [commit-res (-> commit-message
                               build-vcs/commit-chain-cmd
                               (force-dirs dir)
                               (chain-cmds "Commit has failed." verbose)
                               build-commands/first-failing
                               build-vcs/commit-analyze)
                {:keys [nothing-to-commit exit]} commit-res]
            (cond
              nothing-to-commit (normalln "Skipped as no changes found.")
              (not (zero? exit)) (errorln "Unexpected error.")
              :else (normalln "Commit" (build-headers-vcs/latest-commit-sha) "has been created"))
            (success commit-res)))]
    (if commit-status (h1-valid "Commit ok") (h1-error "Commit has failed."))
    (print-writter s)
    (normalln "Commit message: " commit-message)
    commit-status))

(defn synthesis
  "Prints a synthesis line for `status`."
  [status]
  (cond
    (nil? status) nil
    (true? status) (h2-valid! "Commit ok")
    (false? status) (h2-error! "Commit has failed")))
