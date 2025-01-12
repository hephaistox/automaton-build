(ns automaton-build.code.vcs
  "Version Control System.

  Proxy to [git](https://git-scm.com/book/en/v2)."
  (:require
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.filename :as build-filename]
   [clojure.string              :as str]))

(defn- msg-tokenize [s] (str "\"" s "" "\""))

;; Initialize a repo
;; ********************************************************************************

(defn init-vcs-cmd
  "Turns a directory into a local repo"
  ([] ["git" "init" "-q"])
  ([branch] ["git" "init" "-q" "-b" branch]))

(defn remove-pager-cmd
  "Remove pager so branches are printed straightfully"
  []
  ["git" "config" "--local" "pager.branch" "false"])

(defn add-origin-cmd "Add the origin remote" [repo-url] ["git" "remote" "add" "origin" repo-url])

(defn fetch-origin-cmd "Fetch the origin" [] ["git" "fetch" "origin"])

(defn clone-cmd
  "Clone a clone of a remote repo `repo-url`, branch `branch-name`"
  [branch-name repo-url]
  ["git"
   "clone"
   "--single-branch"
   "--branch"
   branch-name
   repo-url
   "--depth"
   "1"
   "--no-checkout"
   "--filter=blob:none"
   "."])

(defn shallow-clone-cmd
  [branch-name repo-url]
  (concat ["git" "clone" repo-url "--single-branch"]
          (when branch-name ["-b" branch-name])
          ["--depth" "1"]))

(defn shallow-clone-analyze
  "Analyse shallow-clone-cmd process and adds `:cloning-status`
  * `:inexisting-remote-branch`
  * `:repository-not-found`
  * `:ok` otherwise"
  [{:keys [err]
    :as process}]
  (cond-> (assoc process :status :ok)
    (re-find #"(?m)Could not find remote branch" err) (assoc :status :inexisting-remote-branch)
    (re-find #"repository .* does not exist" err) (assoc :status :repository-not-found)))

;; Get data from remote repo
;; ********************************************************************************

(defn checkout-file-cmd
  "Checkout `file-name` in branch `branch-name`"
  [branch-name file-name]
  ["git" "checkout" branch-name "--" file-name])

(defn fetch-cmd [branch] ["git" "fetch" "origin" (str branch ":" branch)])

(defn pull-cmd [] ["git" "pull"])

;; Remote repository
;; ********************************************************************************

(defn list-remote-branch-cmd [] ["git" "branch" "-r"])

(defn list-remote-branch-analyze
  [process]
  (->> process
       :out-stream
       str/split-lines
       (remove str/blank?)
       (mapv str/trim)))

(defn remote-branch-exists?-cmd
  "Is the `local-branch` exists on the remote repository"
  [process local-branch]
  (-> process
      list-remote-branch-analyze
      (contains? (str "origin/" local-branch))))

(defn current-repo-url-cmd "Command to return the current remote url" [] ["git" "remote" "-v"])

(defn current-repo-url-analyze
  "Returns the url string to push to the origin repo, `nil` if was failing."
  [{:keys [status out-stream]}]
  (when (= :success status)
    (->> out-stream
         (map (fn [line]
                (->> line
                     (re-find #"origin\s*([^\s]*).*(push)")
                     second)))
         (filter some?)
         first)))

;; Committing
;; ********************************************************************************

(defn add-all-changed-files-cmd [] ["git" "add" "-A"])

(defn commit-cmd [msg] ["git" "commit" "-m" (msg-tokenize msg)])

(defn commit-analyze
  "Analyze the first failing command to tell if it is the commit, and if the commit was succesful.

  * Adds `:nothing-to-commit` when this is the case.
  * Adds `:is-commit`"
  [{:keys [out-stream]
    :as process}]
  (cond-> process
    (re-find #"(?m)nothing to commit" out-stream) (assoc :nothing-to-commit true)))

(defn git-changes?-cmd
  "Returns `true` if directory `dir` is under version control and has pending changes."
  []
  ["git" "status" "-s"])

(defn latest-commit-message-cmd
  "Returns a command to get the commit message of the latest commit of the current branch."
  []
  ["git" "log" "-1" "--pretty=format:%B"])

(defn latest-commit-sha-cmd
  "Returns a command to get the current commit sha for the current branch."
  []
  ["git" "log" "-n" "1" "--pretty=format:%H"])

(defn push-cmd
  "Returns a command to push the local commits of the repo where the command is executed for the branch `branch-name`.

  If `force?` is `true`, the push even if commit are conflicting with the remote branch."
  [branch-name force?]
  (cond-> ["git" "push" "--tags" "--set-upstream" "origin" branch-name]
    force? (conj "--force")))

(defn push-analyze
  "Analyze the `process` of the `push-cmd` to add `:nothing-to-do` if no commit was to be pushed."
  [{:keys [err]
    :as process}]
  (cond-> process
    (re-find #"Everything up-to-date" err) (assoc :nothing-to-push true)))

;; Branches
;; ********************************************************************************

(defn current-branch-cmd
  "Returns a command to get the name of the current branch."
  []
  ["git" "branch" "--show-current"])

(defn current-branch-analyze
  "Return the current branch from one `process` returned value."
  [process]
  (-> process
      :out-stream
      first))

(defn create-branch-cmd [branch-name] ["git" "branch" branch-name])

(defn switch-branch-cmd [branch-name] ["git" "switch" branch-name])

;; Tagging
;; ********************************************************************************

(defn current-tag-cmd
  "Command to return the  the tag of the current commit"
  []
  ["git" "describe" "--exact-match" "--tags"])

(defn current-tag-analyze
  [process]
  (-> process
      (assoc :tag
             (-> process
                 :out-stream
                 first))))

(defn tag-cmd
  "Creates a tag under name `version` and message `tag-msg`."
  ([version] ["git" "tag" "-f" "-a" version])
  ([version tag-msg] ["git" "tag" "-f" "-a" version "-m" (msg-tokenize tag-msg)]))

(defn push-tag-cmd [tag] ["git" "push" "origin" tag])

(defn tag-push-chain-cmd
  [branch-name dir force?]
  [(cond-> ["git" "push" "--tags" "--set-upstream" "origin" branch-name]
     force? (conj "--force"))
   dir])

;; Cleaning
;; ********************************************************************************

(defn clean-state-cmd "Returns a command to detect clean state" [] ["git" "status" "-s"])

(defn clean-hard-cmd
  "Returns the command to clean the project as it has came back to the same state than the repository is freshly downloaded."
  []
  ["git" "clean" "-fqdxi"])

;; VCS distant run
;; ********************************************************************************

(defn gh-run-wip?-cmd "Returns `true` if the workflow is in progress" [] ["gh" "run" "list"])

(defn gh-run-wip?-analyze
  "Add a remote-repo-status field, which is:
  * `:run-ok` if the last run terminated with success
  * `:run-failed` if not
  * `:wip` is it is still in progress"
  [{:keys [status out-stream]}]
  (when (= :success status)
    (when-let [run-feedback (first out-stream)]
      (cond-> {:run-id (->> (str/split run-feedback #"\t")
                            (drop 6)
                            first)}
        (re-find #"completed\tsuccess" run-feedback) (assoc :status :run-ok)
        (re-find #"completed\tfailure" run-feedback) (assoc :status :run-failed)
        (not (re-find #"completed\t" run-feedback)) (assoc :status :wip)))))

;; Various
;; ********************************************************************************

(defn repo-url-regexp
  "Regexp to validate a `repo-url`."
  []
  #"((git|ssh|http(s)?)|(git@[\w\.]+))(:(//)?)([\w\.@\:/\-~]+)(\.git)(/)?")

(defn find-git-repo
  "Search a git repo in `dir` of its parent directories."
  [dir]
  (build-file/search-in-parents dir ".git"))

(defn spit-hook
  "Spit the `content` in the hook called `hook-name` of `app-dir` repo."
  [app-dir hook-name content]
  (let [hook-filename (build-filename/create-file-path app-dir ".git" "hooks" hook-name)]
    (build-file/write-file hook-filename content)
    (-> (build-file/make-executable hook-filename)
        str)))

(defn git-setup-dir
  "Returns the hidden `.git` directory of app in repo `repo-dir`"
  [repo-dir]
  (build-filename/create-dir-path repo-dir ".git"))

;; Merge
;; ********************************************************************************

(defn merge-cmd "Merges `branch1` into `branch2`" [branch1 branch2] ["git" "merge" branch1 branch2])
