(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.code.vcs                 :as build-vcs]
   [automaton-build.echo.headers             :refer [h1 h1-error h1-error! h1-valid normalln]]
   [automaton-build.monorepo.apps            :as build-apps]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.cmds                  :as build-commands]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.filename              :as build-filename]
   [automaton-build.project.compile          :as build-project-compile]
   [automaton-build.project.deps             :as build-deps]
   [automaton-build.project.map              :as build-project-map]
   [automaton-build.project.versioning       :as build-project-versioning]
   [automaton-build.tasks.impl.headers.cmds  :refer [chain-cmds]]
   [automaton-build.tasks.impl.headers.files :as build-headers-files]
   [automaton-build.tasks.impl.headers.vcs   :as build-headers-vcs]
   [clojure.string                           :as str]))

(def cli-opts
  (-> [["-e"
        "--env ENVIRONMENT"
        "env variable e.g. la, production"
        :missing
        "Environment is required, run -e la or -e production"
        :parse-fn
        #(keyword %)]
       ["-f" "--force" "Do not ask about local apps push" :default true :parse-fn not]
       ["-m" "--message" "Message for local apps push" :parse-fn str]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(defn- pull-base-branch
  "Pull base branch, cannot be executed on base branch"
  [app-dir base-branch current-branch]
  (when-not (= current-branch base-branch)
    (-> (build-vcs/pull-changes-chain-cmd base-branch)
        (concat [[(build-vcs/merge-cmd base-branch current-branch)]])
        (build-commands/force-dirs app-dir)
        build-commands/chain-cmds
        build-commands/first-failing
        build-commands/success)))

(defn clean-state?
  [app-dir]
  (-> (build-vcs/git-changes?-cmd)
      (build-commands/blocking-cmd app-dir)
      build-vcs/git-changes?-analyze
      not))

;;push branch
(defn- prepare-cloned-repo-on-branch
  "Clone the repo in diectory `tmp-dir`, the repo at `repo-address` is copied on branch `branch-name`
  Params:
  * `tmp-dir`
  * `repo-address`
  * `branch-name`"
  [tmp-dir repo-address branch-name]
  (if (build-headers-vcs/clone-repo-branch tmp-dir repo-address branch-name false)
    true
    (when (build-headers-vcs/clone-repo-branch tmp-dir repo-address false)
      (build-headers-vcs/new-branch-and-switch tmp-dir branch-name false))))

(defn- target-branch-git-dir
  "Returns '.git' directory from `target-branch`"
  [repo-address target-branch]
  (let [tmp-dir (build-file/create-temp-dir)]
    (when (prepare-cloned-repo-on-branch tmp-dir repo-address target-branch)
      (build-filename/create-dir-path tmp-dir ".git"))))

(defn- replace-repo-git-dir
  [new-git-dir repo-dir]
  (let [dir-to-push-git-dir (build-filename/create-dir-path repo-dir ".git")]
    (build-file/delete-dir dir-to-push-git-dir)
    (build-headers-files/copy-files new-git-dir dir-to-push-git-dir "*" false {})))

(defn- replace-branch-files
  "Replaces files from target-branch with files from files-dir. Returns directory in which it resides"
  [files-dir repo-address target-branch]
  (let [target-git-dir (target-branch-git-dir repo-address target-branch)
        dir-with-replaced-files (build-file/create-temp-dir)]
    (build-headers-files/copy-files files-dir dir-with-replaced-files "*" false {})
    (replace-repo-git-dir target-git-dir dir-with-replaced-files)
    dir-with-replaced-files))

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
  ([source-dir repo-address force? target-branch commit-msg]
   (let [dir-to-push (replace-branch-files source-dir repo-address target-branch)
         {:keys [exit]
          :as res}
         (-> (or commit-msg "automatic commit")
             build-vcs/commit-chain-cmd
             (concat [[(build-vcs/push-cmd (build-headers-vcs/current-branch dir-to-push) force?)]])
             (build-commands/force-dirs dir-to-push)
             (chain-cmds "Impossible to commit" false)
             build-commands/first-failing)]
     (prn "res: " res)
     (normalln "hello")
     (normalln "hello")
     (case exit
       (1 0 nil) {:status :success}
       :else {:status :failed
              :data res}))))

(defn push-current-branch
  "Pushes `app` current changes to it's repository. The changes are pushed to the current branch of user running the function. It is forbidden to push to base-branch of application."
  [app-dir app-name repo main-branch current-branch force? message]
  (cond
    (not-every? some? [app-dir repo main-branch current-branch])
    {:status :failed
     :data {:app-dir app-dir
            :repo repo
            :main-branch main-branch
            :current-branch current-branch}
     :msg (str app-name " failed due to missing value")}
    (= current-branch main-branch)
    {:status :failed
     :data {:current-branch current-branch
            :main-branch main-branch}
     :msg (format "`%s` failed as branch name `%s` is the same as main branch of the app"
                  app-name
                  current-branch)}
    :else (push-local-dir-to-repo app-dir
                                  repo
                                  force?
                                  current-branch
                                  (when (and message (string? message) (not (str/blank? message)))
                                    message))))

(comment
  (def monorepo-project-map
    (-> (build-project-map/create-project-map "")
        build-project-map/add-project-config
        (build-apps/add-monorepo-subprojects :default)
        (build-apps/apply-to-subprojects build-project-map/add-deps-edn
                                         build-project-map/add-project-config)))
  (def current-branch (build-headers-vcs/current-branch ""))
  current-branch
  ;
  (def automaton-build-config (first (:subprojects monorepo-project-map)))
  (push-current-branch
   (:app-dir automaton-build-config)
   (:app-name automaton-build-config)
   (get-in automaton-build-config [:project-config-filedesc :edn :publication :repo-url])
   (get-in automaton-build-config [:project-config-filedesc :edn :publication :base-branch])
   current-branch
   false
   nil)
  ;
)

;; this for deployment
;; (defn push-base-branch
;;   "Pushes app to `base-branch`"
;;   [app-name app-dir repo base-branch version message]
;;   (let [github-new-changes-link (str "https://github.com/" (first (re-find #"(?<=:)(.*)(?=.git)"
;;                                                                            repo))
;;                                      "/tree/" base-branch)]
;;     (build-log/info-format "Pushing app: `%s` to branch: `%s`. \nChanges can be seen here `%s`"
;;                            app-name
;;                            base-branch
;;                            github-new-changes-link)
;;     (build-cfg-mgt/push-local-dir-to-repo {:source-dir app-dir
;;                                            :repo-address repo
;;                                            :tag {:id version
;;                                                  :msg message}
;;                                            :commit-msg message
;;                                            :force? true
;;                                            :target-branch base-branch})))

(defn current-branch-name-invalid?
  [subapps current-branch]
  (let [base-branches
        (distinct (mapcat (fn [app]
                            [(get-in app [:project-config-filedesc :edn :publication :base-branch])
                             (get-in app [:project-config-filedesc :edn :publication :la-branch])])
                   subapps))]
    (some #(= current-branch %) base-branches)))

(defn ensure-no-cache
  [new-changes-branch repo]
  (let [tmp-dir (build-file/create-temp-dir)]
    (build-headers-vcs/clone-repo-branch tmp-dir repo new-changes-branch false)
    tmp-dir))

(defn deploy*
  [app current-branch repo env]
  (let [app-dir (ensure-no-cache current-branch repo)
        app-name (:app-name app)
        class-dir (build-filename/absolutize
                   (build-filename/create-dir-path app-dir (format "target/%s/class/" (name env))))
        target-jar-filename (build-filename/create-file-path
                             (format "target/%s/%s.jar" (name env) app-name))
        excluded-aliases (get-in app [:project-config-filedesc :edn :publication :excluded-aliases])
        paths (map #(build-filename/absolutize (build-filename/create-dir-path app-dir %))
                   (build-deps/extract-paths (get-in app [:deps :edn]) excluded-aliases))
        shadow-deploy-alias
        (get-in app [:project-config-filedesc :edn :publication :shadow-cljs-deploy-alias])
        css-files [:project-config-filedesc :edn :publication :css-files]
        compiled-css-path [:project-config-filedesc :edn :publication :compiled-css-path]
        compile-jar (get-in app [:project-config-filedesc :edn :publication :compile-jar])
        compile-uber-jar (get-in app [:project-config-filedesc :edn :publication :compile-uber-jar])
        jar-entrypoint (get-in app
                               [:project-config-filedesc :edn :publication :uber-jar :entrypoint])
        java-opts (get-in app [:project-config-filedesc :edn :publication :uber-jar :java-opts])]
    (cond-> {}
      true (assoc :app-dir app-dir)
      true (assoc :class-dir class-dir)
      shadow-deploy-alias (assoc :shadow-cljs-compilation
                                 (build-project-compile/shadow-cljs app-dir shadow-deploy-alias))
      (and css-files compiled-css-path)
      (assoc :css-compilation (build-project-compile/css app-dir css-files compiled-css-path))
      compile-jar (assoc
                   :jar-res
                   (build-project-compile/compile-jar class-dir paths target-jar-filename app-dir))
      compile-uber-jar (assoc :uber-jar-res
                              (build-project-compile/compile-uber-jar class-dir
                                                                      paths
                                                                      target-jar-filename
                                                                      app-dir
                                                                      jar-entrypoint
                                                                      java-opts)))))

(defn deploy
  "Deploys app in isolation to ensure no cache which requires that your state should be clean and current branch is not a base branch

   The process of deployment is:
   1. Compile jar
   2. Push changes to app target branch for that environment
   3. Deploy app

  It's done currently not as a workflow, only because workflow can't be used in another workflow and we have a logic we want to preserve here"
  [app env current-branch]
  (let [repo (get-in app [:project-config-filedesc :edn :publication :repo-url])
        target-branch (get-in app
                              [:project-config-filedesc
                               :edn
                               :publication
                               (cond
                                 (= :la env) :la-branch
                                 (= :production env) :base-branch
                                 :else :imnothere)])
        app-dir (:app-dir app)]
    (if (and repo
             target-branch
             (build-project-versioning/version-changed? app-dir repo target-branch env))
      (deploy* app current-branch repo env)
      {:status :skipped
       :data {:repo repo
              :target-branch target-branch}
       :msg (if (and repo target-branch) "No changes found" "Missing parameters")})))

(defn run-monorepo
  []
  (let [monorepo-project-map (-> (build-project-map/create-project-map "")
                                 build-project-map/add-project-config
                                 (build-apps/add-monorepo-subprojects :default)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-deps-edn
                                  build-project-map/add-project-config))
        env (get-in cli-opts [:options :env])
        force (get-in cli-opts [:options :force])
        message (get-in cli-opts [:options :message])
        base-branch (get-in monorepo-project-map
                            [:project-config-filedesc :edn :publication :base-branch])
        app-dir (get-in monorepo-project-map [:app-dir])
        current-branch (build-headers-vcs/current-branch app-dir)
        subapps (->> monorepo-project-map
                     :subprojects)]
    (if-not (pull-base-branch app-dir base-branch current-branch)
      (do
        (h1-error!
         "Current branch is not up-to-date with base, first pull changes from monorepo main branch")
        1)
      (if-not (clean-state? app-dir)
        (do (h1-error! "Git status is not empty. First commmit your changes.") 1)
        (if (current-branch-name-invalid? subapps current-branch)
          (do (h1-error! "Monorepo current branch is same as base branch of subapp") 1)
          (let [push-current-branch-subapps
                (mapv (fn [{:keys [app-dir app-name]
                            :as app}]
                        (h1 app-name " being pushed to " current-branch)
                        (let [app-base-branch
                              (get-in app [:project-config-filedesc :edn :publication :base-branch])
                              repo (get-in app
                                           [:project-config-filedesc :edn :publication :repo-url])
                              push-res (push-current-branch app-dir
                                                            app-name
                                                            repo
                                                            app-base-branch
                                                            current-branch
                                                            force
                                                            message)]
                          (if (= :success (:status push-res))
                            (h1-valid app-name "pushed")
                            (h1-error app-name "push failed with: " push-res))
                          push-res))
                      subapps)]
            (if-not (every? #(= :success (:status %)) push-current-branch-subapps)
              (do (h1-error! "Local push failed") 1)
              (let [deploy-res (mapv #(-> (deploy % env current-branch)
                                          (assoc :app-name (:app-name %)))
                                     subapps)]
                deploy-res))))))))

(comment
  (run-monorepo)
  ;
)
