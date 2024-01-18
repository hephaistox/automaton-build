(ns automaton-build.cicd.cfg-mgt
  "Adapter for configuration management

  Proxy to git"
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.os.commands :as build-cmds]
   [automaton-build.os.files :as build-files]
   [clojure.string :as str]))

(defn latest-commit-message
  []
  (first (build-cmds/execute-get-string
          ["git" "log" "-1" "--pretty=format:%B"])))

(defn git-installed?*
  "Returns true if git is properly installed
  Params:
  * `git-cmd` is an optional parameter to give an alternative git command"
  ([] (git-installed?* "git"))
  ([git-cmd]
   (try (or (zero? (ffirst (build-cmds/execute-with-exit-code
                            [git-cmd "-v" {:dir "."}])))
            (do (build-log/error "Git command does not work") false))
        (catch Exception e (build-log/error-exception e) false))))

(def git-installed?
  "Returns true if git is properly installed
  That version executes only once"
  (memoize git-installed?*))

(defn clean-hard
  "Configuration management comes back to the same state than the repository is freshly donwloaded
   Returns true if cleaning suceeded.
   Params:
  * `root-dir` is the repository where the cleansing is done
  * `interactive?` true by default, meaning the user is asked to confirm.
  Use `interactive?`=false with caution!!!!!"
  ([root-dir interactive?]
   (build-log/debug-format "Clean the repository `%s`" root-dir)
   (if (git-installed?)
     (build-cmds/execute-and-trace ["git"
                                    "clean"
                                    (str "-fqdx" (when interactive? "i"))
                                    {:dir root-dir
                                     :error-to-std? true}])
     (do (build-log/warn "Clean cannot be done as git is not installed") nil)))
  ([root-dir] (clean-hard root-dir true)))

(defn clone-repo-branch
  "Clone one branch of a remote repository to the `target-dir`
  Params:
  * `target-dir` is the directory where the repository should be cloned
  * `repo-dir-name` (optional) name of cloned directory
  * `repo-address` the remote url where the repository is stored
  * `branch-name` is the name of the branch to download
  Return true if succesfull"
  ([target-dir repo-dir-name repo-address branch-name]
   (build-log/debug-format "Clone in repo `%s`, branch `%s` in `%s` "
                           repo-address
                           branch-name
                           target-dir)
   (when (git-installed?)
     (let [[exit-code message] (->> (build-cmds/execute-with-exit-code
                                     ["git"
                                      "clone"
                                      repo-address
                                      repo-dir-name
                                      "--single-branch"
                                      "-b"
                                      branch-name
                                      "--depth"
                                      "1"
                                      {:dir target-dir}])
                                    first)]
       (cond
         (zero? exit-code) true
         (re-find #"Could not find remote branch" message)
         (do (build-log/error-format "Branch `%s` does not exists in repo `%s`"
                                     branch-name
                                     repo-address)
             false)
         (re-find #"Repository not found" message)
         (do (build-log/error-format "Repository `%s` not found" repo-address)
             false)
         :else (do (build-log/error "Unexpected error during clone repo: "
                                    message)
                   false)))))
  ([target-dir repo-address branch-name]
   (clone-repo-branch target-dir target-dir repo-address branch-name)))

(defn create-and-switch-to-branch
  "In an existing repo stored in `dir`, creates a branch called `branch-name` and switch to it
  Params:
  * `dir` directory where the repo to update lies
  * `branch-name` the branch to create and switch to"
  [dir branch-name]
  (when (git-installed?)
    (let [branch-switch-res (build-cmds/execute-with-exit-code
                             ["git" "branch" branch-name {:dir dir}]
                             ["git" "switch" branch-name {:dir dir}])]
      (if (build-cmds/first-cmd-failing branch-switch-res)
        true
        (do (build-log/error-format "Unexpected error during branch creation %s"
                                    (map second branch-switch-res))
            false)))))

(defn current-branch
  "Return the name of the current branch in `dir`
  Params:
  * `dir` directory where the repository to get the branch from"
  [dir]
  (when (git-installed?)
    (let [result (-> (build-cmds/execute-get-string
                      ["git" "branch" "--show-current" {:dir dir}])
                     first
                     str/split-lines
                     first)]
      (build-log/debug-format
       "Retrieve the current branch in directory `%s`, found = `%s`"
       dir
       result)
      result)))

(defn commit-and-push
  "Push to its `origin` what is the working state in `dir` to branch `branch-name`
 Params:
  * `dir` directory where the repository is stored
  * `msg` message for the commit
  * `branch-name` branch name"
  [dir msg branch-name]
  (let [msg (or msg "commit")]
    (when (git-installed?)
      (let [commit-res
            (build-cmds/execute-with-exit-code
             ["git" "add" "." {:dir dir}]
             ["git" "commit" "-m" msg {:dir dir}]
             ["git" "push" "--set-upstream" "origin" branch-name {:dir dir}])]
        commit-res))))

(defn current-commit-sha
  "Returns the current commit sha in the directory `dir`
  It will look at the currently selected branch
  Params:
  * `dir` directory where the local repo is stored"
  [dir]
  (-> (build-cmds/execute-get-string
       ["git" "log" "-n" "1" "--pretty=format:%H" {:dir dir}])
      first))

(defn commit-and-push-and-tag
  "Push to its `origin` what is the working state in `dir` to branch `branch-name`
 Params:
  * `dir` directory where the repository is stored
  * `msg` message for the commit
  * `branch-name` branch name
  * `version` version to use in the tag
  * `tag-msg` is the message of the tag"
  [dir msg branch-name version tag-msg]
  (build-log/info-format "Commit and push in progress in dir `%s`" dir)
  (let [msg (or msg "commit")]
    (when (git-installed?)
      (let [commit-res (build-cmds/execute-with-exit-code
                        ["git" "add" "." {:dir dir}]
                        ["git" "commit" "-m" msg {:dir dir}]
                        ["git" "tag" "-f" "-a" version "-m" tag-msg {:dir dir}]
                        ["git"
                         "push"
                         "--tags"
                         "--set-upstream"
                         "origin"
                         branch-name
                         {:dir dir}])
            [cmd-failing message] (build-cmds/first-cmd-failing commit-res)]
        (build-log/info-format "branch `%s`" (current-branch dir))
        (build-log/info-format "commit `%s`" (current-commit-sha dir))
        (case cmd-failing
          nil (do (build-log/info-format
                   "Succesfully pushed version `%s` version"
                   version)
                  true)
          1 (do (build-log/info-format "Nothing to commit, skip the push")
                false)
          2 (do (build-log/error-format "Tag has failed - %s" message) false)
          3
          (do
            (build-log/error-format
             "Push has failed - %s, you could try to remove the remote tag if this is the issue, and retry:\n`git push -d origin %s`"
             message
             version)
            false)
          :else (do (build-log/error
                     "Unexpected error during commit-and-push : "
                     (into [] commit-res))
                    false))))))

(defn- prepare-cloned-repo-on-branch
  "Clone the repo in diectory `tmp-dir`, the repo at `repo-address` is copied on branch `branch-name`, if it does not exist create a branch based on `base-branch-name`
  Params:
  * `tmp-dir`
  * `repo-address`
  * `base-branch-name`
  * `branch-name`"
  [tmp-dir repo-address base-branch-name branch-name]
  (if (clone-repo-branch tmp-dir repo-address branch-name)
    (do (build-log/debug-format "Succesfully cloned branch %s" branch-name)
        true)
    (do
      (build-log/debug-format
       "Branch `%s` does not exist on the remote repo, it will be created locally"
       branch-name)
      (when (clone-repo-branch tmp-dir repo-address base-branch-name)
        (create-and-switch-to-branch tmp-dir branch-name)))))

(defn- squash-local-files-and-push
  "Considering a cloned repo in `tmp-dir`, replace the current files with the ones in `source-dir`
  Remind that monorepo is our the source of truth, so use it with caution
  Params:
  * `tmp-dir` where the cloned repo is stored, the branch should already be sync with remote repo and currently selected
  * `source-dir` the files that will be copied from
  * `commit-message` the message for the commit,
  * `tag` (optional) map containg `id` with tag and optional `msg` with corresponding message
  * `version`"
  [tmp-dir source-dir commit-message tag]
  (when (git-installed?)
    (->> (build-files/search-files tmp-dir "*")
         (filter (fn [file] (not (str/ends-with? file ".git"))))
         build-files/delete-files)
    (when (build-files/copy-files-or-dir [source-dir] tmp-dir)
      (if tag
        (commit-and-push-and-tag tmp-dir
                                 commit-message
                                 (current-branch tmp-dir)
                                 (:id tag)
                                 (:msg tag))
        (commit-and-push tmp-dir commit-message (current-branch tmp-dir))))))

(defn changed?
  "Returns true if there is a difference between state on `base-branch` and `target-branch` with `source-dir` in `repo-address`"
  [source-dir repo-address base-branch target-branch]
  (let [tmp-dir (build-files/create-temp-dir)]
    (when (git-installed?)
      (when (prepare-cloned-repo-on-branch tmp-dir
                                           repo-address
                                           base-branch
                                           target-branch)
        (->> (build-files/search-files tmp-dir "*")
             (filter (fn [file] (not (str/ends-with? file ".git"))))
             build-files/delete-files)
        (build-files/copy-files-or-dir [source-dir] tmp-dir)
        (let [res (first (build-cmds/execute-get-string
                          ["git" "status" "-s" {:dir tmp-dir}]))]
          (build-log/debug "Affected files" res)
          (not (str/blank? res)))))))

(defn- validate-branch-name
  "Validate the name of the branch between `base-branch` and `other-branch` branches are forbid except if `force?` is ok
  * `force?` boolean to force
  * `base-branch`
  * `other-name`
  Returns true if the name is validated"
  [force? base-branch other-branch]
  (if (and (not force?) (= base-branch other-branch))
    (do
      (build-log/error
       "Push to main or master is refused. If you want to confirm, use -f option")
      false)
    true))

(defn remote-branches
  "Return the remote branches for a repo
  This work manually, but for a weird reason this is not working here

  Params:
  * `repo-url` The url of the repo to download"
  [repo-url]
  (let [tmp-dir (build-files/create-temp-dir)]
    (build-log/trace-format "Create a repo `%s` to check for remote branches"
                            tmp-dir)
    (build-cmds/execute-and-trace
     ["git" "init" "-q" {:dir tmp-dir}]
     ["git" "config" "--local" "pager.branch" "false" {:dir tmp-dir}]
     ["git" "remote" "add" "origin" repo-url {:dir tmp-dir}])
    (build-cmds/execute-get-string ["git" "branch" "-aqr" {:dir tmp-dir}])))

(defn push-local-dir-to-repo
  "Use that function to push the files in the `source-dir` to the repo
  Params:
  * `source-dir` local directory where the sources are stored, before being pushed to the remote repo
  * `repo-address` the address of the repo where to push
  * `base-branch-name` if branch-name does not exist, it will be created based on `base-branch-name`
  * `commit-msg` message that will end in pushed commit
  * `version` string with tag version
  * `tag` (optional) map containg `id` with tag and optional `msg` with corresponding message
  * `force?` (optional default false) if false, will refuse to push on master or main branches
  * `target-branch` (optional default current-branch) where to push"
  ([{:keys [source-dir
            repo-address
            base-branch-name
            commit-msg
            tag
            force?
            target-branch]
     :or {target-branch (current-branch ".")}}]
   (when (git-installed?)
     (build-log/info "Pushing from local directory to repository")
     (build-log/trace-map "Push local directories"
                          :source-dir source-dir
                          :repo-address repo-address
                          :base-branch-name base-branch-name
                          :branch-name target-branch
                          :commit-msg commit-msg
                          :tag tag
                          :force? force?)
     (when (validate-branch-name base-branch-name force? target-branch)
       (let [tmp-dir (build-files/create-temp-dir)]
         (when (prepare-cloned-repo-on-branch tmp-dir
                                              repo-address
                                              base-branch-name
                                              target-branch)
           (build-log/debug
            "Pushing from local directory to repository - repo is ready")
           (squash-local-files-and-push tmp-dir source-dir commit-msg tag)))))))

(defn find-git-repo
  "Search in the parent directories if
  Params:
  * `dir` directory where to start the search"
  [dir]
  (build-files/search-in-parents dir ".git"))

(defn spit-hook
  "Spit the `content` in the hook called `hook-name`
  Params:
  * `app-dir` will search in git repository here or in the first parent which is a repo
  * `hook-name`
  * `content`"
  [app-dir hook-name content]
  (let [hook-filename
        (-> (find-git-repo app-dir)
            (build-files/create-file-path ".git" "hooks" hook-name))]
    (build-log/trace-format "Creating hook `%s`" hook-filename)
    (build-files/spit-file hook-filename content)
    (build-files/make-executable hook-filename)))
