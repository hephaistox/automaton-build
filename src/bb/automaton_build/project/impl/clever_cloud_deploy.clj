(ns automaton-build.project.impl.clever-cloud-deploy
  "Code here is regarding all interactions with clever cloud.
   Design decision:
   Interaction with clever cloud is by using git.

   Alternatively it could be done by using clever CLI, but currently we don't need any features that can't be done by simply manipulating the repository in the git. Which is a simpler option in terms of complexity (no need for API to check), while still being flexible (a lot of options to manipulate in git). Additionally this doesn't require to install the clever cloud cli."
  (:require
   [automaton-build.code.vcs    :as build-vcs]
   [automaton-build.os.cmds     :as build-commands]
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.filename :as build-filename]))


(defn clone-repo
  "Clones clever repo from `uri` to a `target-dir`, repo will be stored as `repo-name`.  Returns the path to the repository."
  [target-dir uri repo-name]
  (let [clever-repo-dir (build-filename/create-dir-path target-dir repo-name)]
    (build-file/delete-path clever-repo-dir)
    (build-file/ensure-dir-exists clever-repo-dir)
    (let [clone-res (-> (build-vcs/shallow-clone-repo-branch-cmd uri "master" repo-name)
                        (build-commands/blocking-cmd target-dir))]
      (if (-> clone-res
              build-commands/success)
        {:status :success
         :filepath clever-repo-dir}
        {:status :failed
         :res clone-res
         :filepath clever-repo-dir}))))

(defn deploy
  "Deploys to clever-cloud, returns true if succesfull.
   Params:
   * `dir` where clever git repository that should be deployed resides.
   * `msg` (optional) a commit message for the deploy"
  ([dir msg]
   (-> msg
       build-vcs/commit-chain-cmd
       (concat [[(build-vcs/push-cmd "master" true)]])
       (build-commands/force-dirs dir)
       build-commands/chain-cmds
       build-commands/first-failing))
  ([dir] (deploy dir "Automatically pushed version")))
