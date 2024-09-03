(ns automaton-build.tasks.impl.headers.vcs
  (:require
   [automaton-build.code.vcs                :as build-vcs]
   [automaton-build.echo.headers            :refer [errorln h2 h2-error h2-error! h2-valid uri-str]]
   [automaton-build.os.file                 :as build-file]
   [automaton-build.tasks.impl.headers.cmds :refer [blocking-cmd chain-cmds force-dirs success]]
   [clojure.string                          :as str]))

(defn remote-branches
  "Returns remote branches in `repo-url`."
  [repo-url verbose]
  (let [target-dir (build-file/create-temp-dir "headers-vcs-test")]
    (-> (build-vcs/remote-branches-chain-cmd repo-url)
        (force-dirs target-dir)
        (chain-cmds "Remote branch has failed." verbose)
        build-vcs/remote-branches)))

(defn remote-branch-exists?
  "Is the `local-branch` exists on the remote repository at `repo-url."
  [remote-branches local-branch]
  (build-vcs/remote-branch-exists? remote-branches local-branch))

(defn clone-repo-branch
  "Clone `branch` in the `repo-url` in the directory `target-dir`."
  [target-dir repo-url branch verbose]
  (if-not (str/blank? branch)
    (let [s (new java.io.StringWriter)
          _ (h2 "Clone repo with branch" (uri-str branch))
          res (binding [*out* s]
                (-> (build-vcs/shallow-clone-repo-branch-cmd repo-url branch)
                    (blocking-cmd target-dir "Impossible to clone." verbose)))]
      (if (success res)
        (h2-valid "Branch" (uri-str branch) "cloning is successfull.")
        (h2-error "Branch" (uri-str branch) "cloning is not successfull."))
      (let [s (str s)] (when-not (str/blank? s) (print s)))
      (success res))
    (h2-error! "No branch provided.")))

(defn new-branch-and-switch
  "In the repository in directory `repo-dir`, creates a new branch."
  [repo-dir branch verbose]
  (-> (build-vcs/new-branch-and-switch-chain-cmd branch)
      (force-dirs repo-dir)
      (chain-cmds "Error during branch creation." verbose)))

(defn create-empty-branch
  "Download repo at `repo-url`, and creates `branch` based on latest commit of base-branch,

  Which content is completly removed."
  [repo-dir repo-url branch base-branch verbose]
  (if (str/blank? repo-url)
    (do (errorln "Unexpectedly empty `repo-url`") nil)
    (do (clone-repo-branch repo-dir repo-url base-branch verbose)
        (new-branch-and-switch repo-dir branch verbose)
        (->> (build-file/search-files repo-dir "*" {:hidden true})
             (remove #(or (str/ends-with? % ".git/") (str/ends-with? % ".git")))
             (mapv build-file/delete-path))
        repo-dir)))

(defn push
  [branch-name repo-dir force? verbose]
  (-> (build-vcs/push-cmd branch-name force?)
      (blocking-cmd repo-dir "Push has failed" verbose)
      build-vcs/push-analyze))

(defn current-branch
  "Returns the string of the current branch."
  []
  (-> (build-vcs/current-branch-cmd)
      (blocking-cmd "." "Current branch command has failed" false)
      build-vcs/current-branch-analyze))

(defn latest-commit-sha
  "Returns the string of the current branch."
  []
  (-> (build-vcs/latest-commit-sha-cmd)
      (blocking-cmd "." "Current branch command has failed" false)
      :out))
