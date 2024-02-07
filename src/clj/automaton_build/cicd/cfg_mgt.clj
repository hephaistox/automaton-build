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
   (build-log/trace-format "Clone in repo `%s`, branch `%s` in `%s` "
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
                                      (when branch-name "-b")
                                      (when branch-name branch-name)
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
      (build-log/trace-format
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
  ([dir msg branch-name force?]
   (let [msg (or msg "commit")]
     (when (git-installed?)
       (let [commit-res (build-cmds/execute-with-exit-code
                         ["git" "add" "-A" {:dir dir}]
                         ["git" "commit" "-m" msg {:dir dir}]
                         ["git"
                          "push"
                          "--set-upstream"
                          "origin"
                          branch-name
                          (when force? "--force")
                          {:dir dir}])]
         commit-res))))
  ([dir msg branch-name] (commit-and-push dir msg branch-name false)))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
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
  ([dir msg branch-name version tag-msg]
   (commit-and-push-and-tag dir msg branch-name version tag-msg false))
  ([dir msg branch-name version tag-msg force?]
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
                          (when force? "--force")
                          {:dir dir}])
             [cmd-failing message] (build-cmds/first-cmd-failing commit-res)]
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
              "Push has failed with message: %s. If needed you can drop the tag with: `git push -d origin %s`"
              message
              version)
             false)
           :else (do (build-log/error
                      "Unexpected error during commit-and-push : "
                      (into [] commit-res))
                     false)))))))

(defn- prepare-cloned-repo-on-branch
  "Clone the repo in diectory `tmp-dir`, the repo at `repo-address` is copied on branch `branch-name`
  Params:
  * `tmp-dir`
  * `repo-address`
  * `branch-name`"
  [tmp-dir repo-address branch-name]
  (if (clone-repo-branch tmp-dir repo-address branch-name)
    (do (build-log/debug-format "Succesfully cloned branch %s" branch-name)
        true)
    (do
      (build-log/debug-format
       "Branch `%s` does not exist on the remote repo, it will be created locally"
       branch-name)
      (when (clone-repo-branch tmp-dir repo-address nil)
        (create-and-switch-to-branch tmp-dir branch-name)))))

(defn- replace-repo-git-dir
  [new-git-dir repo-dir]
  (let [dir-to-push-git-dir (build-files/create-dir-path repo-dir ".git")]
    (build-files/delete-files dir-to-push-git-dir)
    (build-files/copy-files-or-dir [new-git-dir] dir-to-push-git-dir)))

(defn pull-changes
  [branch dir]
  (build-cmds/execute-with-exit-code
   ["git" "fetch" "origin" (str branch ":" branch) {:dir dir}]
   ["git" "pull" {:dir dir}]
   ["git" "merge" branch (current-branch dir) {:dir dir}]))

(defn- target-branch-git-dir
  "Returns '.git' directory from `target-branch`"
  [repo-address target-branch]
  (let [tmp-dir (build-files/create-temp-dir)]
    (when (prepare-cloned-repo-on-branch tmp-dir repo-address target-branch)
      (build-files/create-dir-path tmp-dir ".git"))))

(defn- replace-branch-files
  "Replaces files from target-branch with files from files-dir. Returns directory in which it resides"
  [files-dir repo-address target-branch]
  (let [target-git-dir (target-branch-git-dir repo-address target-branch)
        dir-with-replaced-files (build-files/create-temp-dir)]
    (build-files/copy-files-or-dir [files-dir] dir-with-replaced-files)
    (replace-repo-git-dir target-git-dir dir-with-replaced-files)
    dir-with-replaced-files))

(defn git-changes?
  [dir]
  (let [res (build-cmds/execute-get-string ["git" "status" "-s" {:dir dir}])]
    (build-log/trace-format "in directory `%s` git status result %s" dir res)
    (not (str/blank? (first res)))))

(defn changed?
  "Returns true if there is a difference between state on `branch` and `source-dir`"
  [source-dir repo-address branch]
  (when (git-installed?)
    (when-let [replaced-dir
               (replace-branch-files source-dir repo-address branch)]
      (git-changes? replaced-dir))))

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
  "Commit and push `target-branch` with files from `source-dir`.
  Params:
  * `source-dir` local directory where the sources are stored, before being pushed to the remote repo
  * `repo-address` the address of the repo
  * `commit-msg` message that will end in pushed commit
  * `version` string with tag version
  * `tag` (optional) map containg `id` with tag and optional `msg` with corresponding message
  * `force?` (optional default false) if true, will force the changes to be pushed as top commit
  * `target-branch` (optional default current-branch) where to push"
  ([{:keys [source-dir repo-address commit-msg tag force? target-branch]
     :or {target-branch (current-branch ".")}}]
   (when (git-installed?)
     (build-log/debug-format "Pushing from `%s` to repository `%s`"
                             source-dir
                             repo-address)
     (let [dir-to-push
           (replace-branch-files source-dir repo-address target-branch)]
       (if tag
         (commit-and-push-and-tag dir-to-push
                                  commit-msg
                                  (current-branch dir-to-push)
                                  (:id tag)
                                  (:msg tag)
                                  force?)
         (commit-and-push dir-to-push
                          commit-msg
                          (current-branch dir-to-push)
                          force?))))))

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
