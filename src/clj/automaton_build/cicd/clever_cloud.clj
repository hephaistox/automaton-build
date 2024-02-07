(ns automaton-build.cicd.clever-cloud
  "Code here is regarding all interactions with clever cloud.
   Design decision:
   Interaction with clever cloud is by using git.

   Alternatively it could be done by using clever CLI, but currently we don't need any features that can't be done by simply manipulating the repository in the git. Which is a simpler option in terms of complexity (no need for API to check), while still being flexible (a lot of options to manipulate in git). Additionally this doesn't require to install the clever cloud cli."
  (:require
   [automaton-build.cicd.cfg-mgt :as build-cfg-mgt]
   [automaton-build.log :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.files :as build-files]))

(defn clone-repo
  "Clones clever repo from `uri` to a `target-dir`, repo will be stored as `repo-name`.  Returns the path to the repository."
  [target-dir uri repo-name]
  (let [clever-repo-dir (build-files/create-dir-path target-dir repo-name)]
    (build-files/delete-files [clever-repo-dir])
    (build-files/ensure-directory-exists clever-repo-dir)
    (if (build-cfg-mgt/clone-repo-branch target-dir "repo" uri "master")
      (build-log/debug "Clever repo cloned succesfully")
      (build-log/warn
       "Clever repo clone failed, please make sure you have locally acess to the clever cloud repositories."))
    clever-repo-dir))

(defn deploy
  "Deploys to clever-cloud, returns true if succesfull.
   Params:
   * `dir` where clever git repository that should be deployed resides.
   * `msg` (optional) a commit message for the deploy"
  ([dir msg]
   (let [commit-res (build-cfg-mgt/commit-and-push dir msg "master")]
     (case (first (build-cmds/first-cmd-failing commit-res))
       nil (do (build-log/info "Successfully published") true)
       1 (do (build-log/debug "No new files to publish, skip the push") true)
       (do (build-log/error "Unexpected error during publishing : "
                            (into [] commit-res))
           false))))
  ([dir] (deploy dir "Automatically pushed version")))

(comment
 ;;
)
