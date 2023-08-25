(ns automaton-build.adapters.cfg-mgt
  "Configuration management of code
  Proxy to git"
  (:require
   [clojure.string :as str]

   [automaton-build.adapters.commands :as cmds]
   [automaton-build.adapters.edn-utils :as edn-utils]
   [automaton-build.adapters.log :as log]))

(defn git-installed?*
  "Returns true if git is properly installed
  Params:
  * `git-cmd` is an optional parameter to give an alternative git command"
  ([]
   (git-installed?* "git"))
  ([git-cmd]
   (when-let [exception  (try
                           (cmds/exec-cmds [[[git-cmd "-v"]]]
                                           {:out :string
                                            :dir "."})
                           nil
                           (catch clojure.lang.ExceptionInfo e
                             e))]
     (throw (ex-info "Git is not working, aborting, is expecting command"
                     {:exception exception
                      :git-cmd git-cmd})))
   true))

(def git-installed?
  "Returns true if git is properly installed
  That version executes only once"
  (memoize git-installed?*))

(defn gh-installed?*
  "Returns true if gh cli is properly installed
  Params:
  * `gh-cmd` is an optional parameter to give an alternative github command"
  ([]
   (gh-installed?* "gh"))
  ([gh-cmd]
   (when-let [exception  (try
                           (cmds/exec-cmds [[[gh-cmd "status"]]]
                                           {:out :string
                                            :dir "."})
                           nil
                           (catch clojure.lang.ExceptionInfo e
                             e))]
     (throw (ex-info "Github cli is not working, aborting, is expecting command. Run `brew install gh` and `gh auth login` "
                     {:exception exception
                      :gh-cmd gh-cmd})))
   true))

(def gh-installed?
  "Returns true if gh cli is properly installed
That version executes only once"
  (memoize gh-installed?*))

(defn clean-to-repo
  "Configuration management comes back to like the repository is freshly donwloaded
  Params:
  * `root-dir` is the repository where the cleansing is done
  * `interactive?` true by default, meaning the user is asked to confirm.
  Use `interactive?`=false with caution!!!!!"
  ([root-dir interactive?]
   (git-installed?)
   (log/debug "Clean the repository")
   (cmds/exec-cmds [[["git" "clean" (str "-fqdx" (when interactive?
                                                   "i")) "-e" ".idea/" "-e" "*.iml"]]]
                   {:dir root-dir}))
  ([root-dir]
   (clean-to-repo root-dir true)))

(defn create-remote-repo
  "Create the remote repository
  Params:
  * `repo-dir` where to execute the command
  * `remote-repo-name` name of the remote repository
  * Returns the adress of the created repository"
  [repo-dir remote-repo-name]
  (gh-installed?)
  (cmds/exec-cmds [[["gh" "repo" "create" remote-repo-name "--private" "--source=."]]]
                  {:out :string
                   :dir repo-dir}))

(defn delete-remote-repo
  "Remove the repository `repo-name`
  Params:
  * `repo-name` is the name of the repository to delete
  * `dir` is the directory where the repo will be cloned"
  [repo-name dir]
  (gh-installed?)
  (log/debug "Remove github repository `" repo-name "`")
  (try
    (-> (cmds/exec-cmds [[["gh" "repo" "delete" repo-name "--yes"]]]
                        {:dir dir
                         :out :string}))
    true
    (catch clojure.lang.ExceptionInfo e
      (if (second (re-find #"HTTP 404: Not Found"
                           (-> e
                               ex-data
                               :exception
                               ex-message)))
        false
        (throw e)))))

(defn extract-remote
  "Extract the remote-url from the returned message of `git remote -v` command
  Params:
  * `remote-msg` is the message returned by the `remote -v` command
  * `remote-name` is the name of the remote
  Returns the address of the remote"
  [remote-msg remote-name]
  (second
   (re-find (re-pattern (str remote-name "\\s*([^\\n\\s]*)\\s*\\(fetch\\)"))
            remote-msg)))

(defn init
  "Init the repository
  Params:
  * `tmp-dir` is the directory where the configuration management should be init"
  [tmp-dir]
  (git-installed?)
  (cmds/exec-cmds [[["git" "init"]]]
                  {:dir tmp-dir
                   :out :string}))

(defn get-remote
  "Get the remote url of the alias `remote-alias` in directory `tmp-dir`
  Params:
  * `tmp-dir` The repository directory where to check the remote branch
  * `remote-alias` The local alias of that remote branch"
  [tmp-dir remote-alias]
  (let [remotes (cmds/exec-cmds [[["git" "remote" "-v"]]]
                                {:dir tmp-dir
                                 :out :string})]
    (extract-remote remotes remote-alias)))

(defn set-origin-to
  "Set the origin of the repo in directory `tmp-dir` to the `repo-address`
  Params:
  * `tmp-dir` where the repository is stored
  * `repo-address` the expected adress of the origin remote
  * `remote-alias` most of the time, it is `origin`"
  [tmp-dir repo-address remote-alias]
  (git-installed?)
  (try
    (if (= (get-remote tmp-dir
                       remote-alias)
           repo-address)
      (do
        (log/debug "Repo `" tmp-dir "`'s remote branch `" remote-alias "`is uptodate")
        :already-ok)
      (do
        (log/warn "Change `" tmp-dir "`'s remote branch " remote-alias " to " repo-address)
        (cmds/exec-cmds [[["git" "remote" "remove" remote-alias]]]
                        {:dir tmp-dir
                         :out :string})
        (cmds/exec-cmds [[["git" "remote" "add" remote-alias repo-address]]]
                        {:dir tmp-dir
                         :out :string})
        :changed))
    (catch Exception e
      (log/warn "Remote fail, we assume the remote repository alias is missing")
      (log/trace (edn-utils/spit-in-tmp-file e))
      (cmds/exec-cmds [[["git" "remote" "add" remote-alias repo-address]]]
                      {:dir tmp-dir
                       :out :string})
      :added)))

(defn clone-repo
  "Clone a remote repository
  Params:
  * `target-dir` is the directory where the repository should be cloned
  * `repo-address` the remote url where the repository is stored
  * `branch-name` is the name of the branch to download"
  [target-dir repo-address branch-name]
  (log/debug "Clone in repo " repo-address ", branch `" branch-name "`in `" target-dir "` ")
  (git-installed?)
  (init target-dir)
  (set-origin-to target-dir repo-address "origin")
  (try
    (cmds/exec-cmds [[["git" "fetch"]]
                     [["git" "pull" "origin" branch-name]]]
                    {:dir target-dir
                     :out :string})
    (catch clojure.lang.ExceptionInfo e
      (log/trace (edn-utils/spit-in-tmp-file (:err (ex-data (:exception (ex-data e))))))
      (let [msg (or (:err (ex-data (:exception (ex-data e))))
                    "")]
        (cond (re-find #"Repository not found" msg) (do
                                                      (log/warn "Fetch has failed for repo `" repo-address "`, with \"repository not found message\", try to create it")
                                                      (cmds/exec-cmds [[["git" "remote" "remove" "origin"]]]
                                                                      {:dir target-dir
                                                                       :out :string})
                                                      (create-remote-repo target-dir repo-address))
              (re-find #"fatal: couldn't find remote ref" msg) (log/warn "Remote branch `" branch-name "` has not been found")
              :else (throw e))))))

(defn is-working-tree-clean?
  "Returns true if the working tree is empty
  Params
  * `dir` where the repository is stored and will be checked
  * `quiet?` (optional, default false) if true, don't display the git status"
  ([dir]
   (is-working-tree-clean? dir false))
  ([dir quiet?]
   (git-installed?)
   (let [git-status (cmds/exec-cmds [[["git" "status"]]]
                                    {:out :string
                                     :dir dir})
         is-clean? (some? (re-find #"nothing to commit" git-status))]
     (when (and (not is-clean?) (not quiet?))
       (log/trace "Git status was: " git-status))
     is-clean?)))

(defn current-branch
  "Return the name of the current branch in `dir`
  Params:
  * `dir` directory where the repository to get the branch from"
  [dir]
  (git-installed?)
  (log/debug "Retrieve the commit id in directory `" dir "`")
  (-> (cmds/exec-cmds [[["git" "branch" "--show-current"]]]
                      {:out :string
                       :dir dir})
      str/split-lines
      first))

(defn push-repo
  "In a `dir` that contains a repository already connected to its origin, presumably with `clone-repo`
  Create a commit of the current modifications and push them to branch `branch-name`
  Params:
  * `repo-dir` the directory of the repository to push
  * `commit-message` the message of the created commit"
  [repo-dir commit-message]
  (git-installed?)
  (let [branch-name (current-branch repo-dir)]
    (try
      (cmds/exec-cmds [[["git" "pull" "origin" branch-name]]]
                      {:dir repo-dir
                       :out :string})
      (catch Exception e
        (log/trace "exception " (edn-utils/spit-in-tmp-file e))
        (log/debug "pull skipped as the branch is new")))
    (cmds/exec-cmds [[["git" "add" "."]]
                     [["git" "commit" "-m" commit-message] {:out :string}]
                     [["git" "push" "--set-upstream" "origin" branch-name]]]
                    {:dir repo-dir
                     :out :string})))

(defn get-commit-id
  "Get the commit id of the repository
  Params:
  * `dir` directory of the repository where to get the commit id from"
  [dir]
  (git-installed?)
  (let [res (cmds/exec-cmds [[["git" "log"]]]
                            {:out :string
                             :dir dir})]
    (-> res
        str/split-lines
        first
        (str/split (re-pattern " "))
        second)))

(defn create-branch
  "Creates a branch.
  If the status is clean.
  * Update both base and target branch before anything else
  * Starts from the `:base-branch` of the monorepo,
  * Creates a branch called `branch-name`
  Params:
  * `repo-dir` where the repo is stored
  * `branch-name` the name of the branch to create
  * `base-branch` is the base branch starting on the `branch-name` one"
  [repo-dir branch-name base-branch]
  (git-installed?)
  (when-not (is-working-tree-clean? repo-dir)
    (throw (ex-info "Please clean your working tree before starting a new branch. Check `git status` for more details"
                    {:cfg-mgt-root-dir repo-dir})))
  (log/trace (cmds/exec-cmds [[["git" "switch" base-branch]]
                              [["git" "fetch" "-p"]]
                              [["git" "pull"]]
                              [["git" "branch" branch-name]]
                              [["git" "switch" branch-name]]]
                             {:dir repo-dir
                              :out :string})))

(defn get-user-name
  "Return the git config user name"
  []
  (git-installed?)
  (let [data (str/trim (cmds/exec-cmds [[["git" "config" "--get" "user.name"]]]
                                       {:out :string
                                        :dir "."}))]
    (log/info "Git user name is " data)
    data))

(defn create-feature-branch
  "Creates a new feature branch called `feature-name` based on `base-branch`, the author is `author-name`
  Params:
  * `repo-dir` a directory in the repository
  * `feature-name` is the name of the feature branch to create
  * `base-branch` is the branch on which the created branch is starting
  Returns the feature branch name"
  [repo-dir feature-name base-branch]
  (create-branch repo-dir
                 (str/join "/" [(get-user-name) "feature" feature-name])
                 base-branch))

(defn change-local-branch
  "Returns true if the branch already exists,
  otherwise, it returns false and creates a feature branch named `current-feature-branch`
  Params:
  * `dir` is the directory where the repository is stored
  * `current-feature-branch` is the name of the branch we would like to create or switch"
  [dir current-feature-branch]
  (git-installed?)
  (try
    (cmds/exec-cmds [[["git" "switch" current-feature-branch]]]
                    {:dir dir
                     :out :string})
    (log/debug "Change to `" current-feature-branch "`")
    true
    (catch Exception _
      (log/trace "Switching failed, try to create the branch now")
      (cmds/exec-cmds [[["git" "branch" "-M" current-feature-branch]]]
                      {:out :string
                       :dir dir})
      false)))
