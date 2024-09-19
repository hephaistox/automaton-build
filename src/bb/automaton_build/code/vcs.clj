(ns automaton-build.code.vcs
  "Version Control System.

  Proxy to [git](https://git-scm.com/book/en/v2)."
  (:require
   [automaton-build.os.file     :as build-file]
   [automaton-build.os.filename :as build-filename]
   [clojure.string              :as str]))

(defn- msg-tokenize [s] (str "\"" s "" "\""))

(defn repo-url-regexp
  "Regexp to validate a `repo-url`."
  []
  #"((git|ssh|http(s)?)|(git@[\w\.]+))(:(//)?)([\w\.@\:/\-~]+)(\.git)(/)?")

(defn latest-commit-message-cmd
  "Returns a command to get the commit message of the latest commit of the current branch."
  []
  ["git" "log" "-1" "--pretty=format:%B"])

(defn latest-commit-sha-cmd
  "Returns a command to get the current commit sha for the current branch."
  []
  ["git" "log" "-n" "1" "--pretty=format:%H"])

(defn current-branch-cmd
  "Returns a command to get the name of the current branch."
  []
  ["git" "branch" "--show-current"])

(defn current-branch-analyze
  "Return the current branch from one `res` returned value."
  [cmd-res]
  (-> cmd-res
      :out
      str/split-lines
      first))

(defn clean-hard-cmd
  "Returns the command to clean the project as it has came back to the same state than the repository is freshly downloaded.

  Use `interactive?`=false with caution!!!!! (default to true) to ask user confirmation."
  ([interactive?] ["git" "clean" (str "-fqdx" (when interactive? "i"))])
  ([] (clean-hard-cmd true)))

(defn git-changes?-cmd
  "Returns `true` if directory `dir` is under version control and has pending changes."
  []
  ["git" "status" "-s"])

(defn git-changes?-analyze
  "Returns `true` if directory `dir` is under version control and has pending changes."
  [cmd-res]
  (-> cmd-res
      :out
      str/blank?
      not))

(defn clone-file-chain-cmd
  "Returns a map with:

  * `file-path` where file will be stored.
  * `chain-cmd`: Chain of command to clone the repo at `repo-url` into the `target-dir` () specific file with it's latest revision.

  It's quick as it ignores all other files, all other branches of the repository and git history."
  [repo-url target-dir branch-name file-name]
  (build-file/ensure-dir-exists target-dir)
  (let [file-path (build-filename/create-dir-path target-dir)]
    [[["git"
       "clone"
       "--single-branch"
       "--branch"
       branch-name
       repo-url
       "--depth"
       "1"
       "--no-checkout"
       "--filter=blob:none"
       "."]
      target-dir]
     [["git" "checkout" branch-name "--" file-name] file-path]]))

(defn shallow-clone-repo-branch-cmd
  "Returns command to clone the repository at address `repo-url` for branch `branch-name` - only the result of the latest commit (i.e. shallow commit). "
  ([repo-url branch-name cloned-dir-name]
   (concat ["git" "clone" repo-url "--single-branch"]
           (when branch-name ["-b" branch-name])
           ["--depth" "1" cloned-dir-name]))
  ([repo-url branch-name] (shallow-clone-repo-branch-cmd repo-url branch-name "."))
  ([repo-url] (shallow-clone-repo-branch-cmd repo-url nil ".")))

(defn shallow-clone-repo-branch-analyze
  "Adds to the `cmd-res` keys `:inexisting-remote-branch` if the repo does not exist, or `:inexisting-remote-branch` if the branch does not exist."
  [cmd-res]
  (let [{:keys [err]} cmd-res]
    (println "err" err)
    (cond-> cmd-res
      (re-find #"(?m)Could not find remote branch" err) (assoc :inexisting-remote-branch true)
      (re-find #"repository .* does not exist" err) (assoc :repository-not-found true))))

(defn pull-changes-chain-cmd
  "Returns a command to fetch and pull changes from `origin`."
  [branch]
  [[["git" "fetch" "origin" (str branch ":" branch)]] [["git" "pull"]]])

(defn merge-cmd "Merges `branch1` into `branch2`" [branch1 branch2] ["git" "merge" branch1 branch2])

(defn new-branch-and-switch-chain-cmd
  "Returns a command "
  [branch-name]
  [[["git" "branch" branch-name]] [["git" "switch" branch-name]]])

(defn commit-chain-cmd
  "Returns a chain of commands to commit all changes in the repo int `dir`, under the message `msg`."
  [msg]
  [[["git" "add" "-A"]] [["git" "commit" "-m" (msg-tokenize msg)]]])

(defn commit-analyze
  "Analyze the first failing command to tell if it is the commit, and if the commit was succesful.

  * Adds `:nothing-to-commit` when this is the case.
  * Adds `:is-commit`"
  [{:keys [cmd-str out]
    :as res}]
  (if (re-find #"commit" cmd-str)
    (cond-> (assoc res :is-commit true)
      (re-find #"(?m)nothing to commit" out) (assoc :nothing-to-commit true))
    res))

(defn push-cmd
  "Returns a command to push the local commits of the repo where the command is executed for the branch `branch-name`.

  If `force?` is `true`, the push even if commit are conflicting with the remote branch."
  [branch-name force?]
  (cond-> ["git" "push" "--tags" "--set-upstream" "origin" branch-name]
    force? (conj "--force")))

(defn push-analyze
  "Analyze the `res` of the `push-cmd` to add `:nothing-to-do` if no commit was to be pushed."
  [{:keys [err]
    :as res}]
  (cond-> res
    (re-find #"Everything up-to-date" err) (assoc :nothing-to-push true)))

(defn tag
  "Creates a tag under name `version` and message `tag-msg`."
  [version tag-msg]
  ["git" "tag" "-f" "-a" version "-m" (msg-tokenize tag-msg)])

(defn tag-push-chain-cmd
  [branch-name dir version tag-msg force?]
  [[["git" "tag" "-f" "-a" version "-m" (msg-tokenize tag-msg)] dir]
   [(cond-> ["git" "push" "--tags" "--set-upstream" "origin" branch-name]
      force? (conj "--force"))
    dir]])

(defn remote-branches-chain-cmd
  "Returns the remote branches for a repo at `repo-url`."
  [repo-url]
  [[["git" "init" "-q"]]
   [["git" "config" "--local" "pager.branch" "false"]]
   [["git" "remote" "add" "origin" repo-url]]
   [["git" "fetch" "origin"]]
   [["git" "branch" "-r"]]])

(defn remote-branches
  [chain-res]
  (some->> chain-res
           last
           :out
           str/split-lines
           (remove str/blank?)
           (mapv str/trim)))

(defn remote-branch-exists?
  "Is the `local-branch` exists on the remote repository at `repo-url."
  [remote-branches local-branch]
  (contains? (set remote-branches) (str "origin/" local-branch)))

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

(defn one-commit-push-chain-cmd
  "Push content of directory `repo-dir` in branch `branch`, and in one commit to repo `ssh-url`."
  [ssh-url branch msg]
  [[["git" "init" "-b" branch]]
   [["git" "add" "-A"]]
   [["git" "commit" "-m" msg]]
   [["git" "remote" "add" "origin" ssh-url]]
   [["git" "add" "."]]
   [["git" "push" "--force" "--set-upstream" "origin" branch]]])

(defn clean-state "Returns a command to detect clean state" [] ["git" "status" "-s"])

(defn clean-state-analyze
  "Check if the returned value of clean state "
  [res]
  (and (= 0 (:exit res)) (str/blank? (:out res))))
