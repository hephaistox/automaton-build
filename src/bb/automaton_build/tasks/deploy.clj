(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.code.vcs                 :as build-vcs]
   [automaton-build.echo.headers             :refer [h1 h1-error h1-error! h1-valid normalln]]
   [automaton-build.monorepo.apps            :as build-apps]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.cmds                  :as build-commands]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.filename              :as build-filename]
   [automaton-build.os.version               :as build-version]
   [automaton-build.project.compile          :as build-project-compile]
   [automaton-build.project.deps             :as build-deps]
   [automaton-build.project.map              :as build-project-map]
   [automaton-build.project.pom-xml          :as build-project-pom-xml]
   [automaton-build.project.publish          :as build-project-publish]
   [automaton-build.project.versioning       :as build-project-versioning]
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
  (if (-> (build-vcs/shallow-clone-repo-branch-cmd repo-address branch-name)
          (build-commands/blocking-cmd tmp-dir)
          build-commands/success)
    true
    (when (-> (build-vcs/shallow-clone-repo-branch-cmd repo-address)
              (build-commands/blocking-cmd tmp-dir)
              (build-commands/success))
      (-> (build-vcs/new-branch-and-switch-chain-cmd branch-name)
          (build-commands/force-dirs tmp-dir)
          build-commands/chain-cmds
          build-commands/first-failing
          build-commands/success))))

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
    (when target-git-dir
      (build-headers-files/copy-files files-dir dir-with-replaced-files "*" false {})
      (replace-repo-git-dir target-git-dir dir-with-replaced-files)
      dir-with-replaced-files)))

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
  ([source-dir repo-address target-branch commit-msg]
   (if-let [dir-to-push (replace-branch-files source-dir repo-address target-branch)]
     (let [branch (build-headers-vcs/current-branch dir-to-push)
           {:keys [exit]
            :as res}
           (-> (or commit-msg "automatic commit")
               build-vcs/commit-chain-cmd
               (concat [[(build-vcs/push-cmd branch true)]])
               (build-commands/force-dirs dir-to-push)
               build-commands/chain-cmds
               build-commands/first-failing)]
       (case exit
         (1 0 nil) {:status :success}
         :else {:status :failed
                :data res}))
     {:status :failed
      :message "Copying local failes for push has failed"})))

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
                                  current-branch
                                  (when (and message (string? message) (not (str/blank? message)))
                                    message))))

(defn push-base-branch
  "Pushes app to `base-branch`"
  [app-name app-dir repo base-branch message]
  (let [github-new-changes-link (str "https://github.com/" (first (re-find #"(?<=:)(.*)(?=.git)"
                                                                           repo))
                                     "/tree/" base-branch)
        message (format "Push: `%s` to branch: `%s`. \nChanges can be seen here `%s`"
                        app-name
                        base-branch
                        github-new-changes-link)]
    (h1 message)
    (let [res (push-local-dir-to-repo app-dir repo base-branch message)]
      (if (= :success (:status res)) (h1-valid message) (h1-error "Push failed " res))
      (= :success (:status res)))))

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
    (when (build-headers-vcs/clone-repo-branch tmp-dir repo new-changes-branch false) tmp-dir)))

(defn deploy*
  [app-dir app-name project-config deps-edn env]
  (let [class-dir (build-filename/absolutize
                   (build-filename/create-dir-path app-dir (format "target/%s/class/" (name env))))
        target-jar-filename (build-filename/create-file-path
                             (format "target/%s/%s.jar" (name env) app-name))
        excluded-aliases (get-in project-config [:publication :excluded-aliases])
        paths (mapv #(build-filename/absolutize (build-filename/create-dir-path app-dir %))
                    (build-deps/extract-paths deps-edn excluded-aliases))
        shadow-deploy-alias (get-in project-config [:publication :shadow-cljs-deploy-alias])
        css-files (get-in project-config [:publication :css-files])
        compiled-css-path [:project-config-filedesc :edn :publication :compiled-css-path]
        compile-jar (get-in project-config [:publication :compile-jar])
        compile-uber-jar (get-in project-config [:publication :compile-uber-jar])
        jar-entrypoint (get-in project-config [:publication :uber-jar :entrypoint])
        java-opts (get-in project-config [:publication :uber-jar :java-opts])
        shadow-res (if shadow-deploy-alias
                     (build-project-compile/shadow-cljs app-dir shadow-deploy-alias)
                     {:status :skipped})
        css-res (if (and css-files compiled-css-path)
                  (build-project-compile/css app-dir css-files compiled-css-path)
                  {:status :skipped})
        jar-res (if compile-jar
                  (build-project-compile/compile-jar class-dir paths target-jar-filename app-dir)
                  {:status :skipped})
        uber-jar-res (if compile-uber-jar
                       (build-project-compile/compile-uber-jar class-dir
                                                               paths
                                                               target-jar-filename
                                                               app-dir
                                                               jar-entrypoint
                                                               java-opts)
                       {:status :skipped})]
    (-> {}
        (assoc :app-dir app-dir)
        (assoc :class-dir class-dir)
        (assoc :shadow-cljs shadow-res)
        (assoc :css css-res)
        (assoc :jar jar-res)
        (assoc :uber-jar uber-jar-res)
        (assoc :general {:status :success}))))

(defn publish-clojars
  [jar-path app-dir]
  (prn "jar-path: " jar-path)
  (prn "pom-path: " (build-project-pom-xml/pom-xml app-dir))
  #_(let [res (build-project-publish/publish-clojars jar-path
                                                     (build-project-pom-xml/pom-xml app-dir))]
      (prn res)
      res))

(defn publish-clever-cloud
  [clever-uri app-dir env]
  (build-project-publish/publish-clever-cloud clever-uri
                                              (->> (name env)
                                                   (build-filename/create-dir-path app-dir "target")
                                                   build-filename/absolutize)
                                              (->> ".clever"
                                                   (build-filename/create-dir-path app-dir)
                                                   build-filename/absolutize)
                                              (build-version/current-version app-dir)))

(defn deploy
  "Deploys app in isolation to ensure no cache which requires that your state should be clean and current branch is not a base branch

   The process of deployment is:
   1. Compile jar
   2. Push changes to app target branch for that environment
   3. Deploy app

  It's done currently not as a workflow, only because workflow can't be used in another workflow and we have a logic we want to preserve here"
  [{:keys [repo target-branch]
    :as app}
   env
   current-branch]
  (if (and repo
           target-branch
           (build-project-versioning/correct-environment? (:app-dir app) env)
           (build-project-versioning/version-changed? (:app-dir app) repo target-branch))
    (if-let [app-dir (ensure-no-cache current-branch repo)]
      (deploy* app-dir
               (:app-name app)
               (get-in app [:project-config-filedesc :edn])
               (get-in app [:deps :edn])
               env)
      {:general {:status :failed
                 :msg "Couldn't ensure that there is no cache"}})
    {:general {:status :skipped
               :data {:repo repo
                      :target-branch target-branch
                      :env env}
               :msg (if (and repo target-branch) "No changes found" "Missing parameters")}}))

(defn publish-apps
  [{:keys
    [app-name app-dir repo target-branch general publish-clojars? publish-cc? jar-res env cc-uri]}]
  (if (= :success (:status general))
    (when
      (push-base-branch app-name app-dir repo target-branch (build-version/current-version app-dir))
      (when publish-clojars? (publish-clojars (:jar-path jar-res) app-dir))
      (when publish-cc? (publish-clever-cloud cc-uri app-dir env)))
    (normalln app-name " publish skipped")))

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
              (let [target-branch-env (cond
                                        (= :la env) :la-branch
                                        (= :production env) :base-branch
                                        :else :imnothere)
                    clever-uri-env (cond
                                     (= :la env) :cc-uri-la
                                     (= :production env) :cc-uri-production
                                     :else :imnotthere)
                    deploy-res
                    (mapv
                     #(-> %
                          (assoc
                           :cc-uri
                           (get-in % [:project-config-filedesc :edn :publication clever-uri-env]))
                          (assoc :env env)
                          (assoc :publish-clojars?
                                 (get-in % [:project-config-filedesc :edn :publication :clojars]))
                          (assoc :publish-cc?
                                 (get-in % [:project-config-filedesc :edn :publication :cc]))
                          (assoc :repo
                                 (get-in % [:project-config-filedesc :edn :publication :repo-url]))
                          (assoc :target-branch
                                 (get-in
                                  %
                                  [:project-config-filedesc :edn :publication target-branch-env]))
                          (deploy env current-branch)
                          (assoc :app-name (:app-name %)))
                     subapps)]
                deploy-res
                #_(if (every? (fn [[_ {:keys [status]}]]
                                (or (= status :skipped) (= status :success)))
                              (select-keys deploy-res [:shadow-cljs :css :jar :uber-jar :general]))
                    (let [push-res (mapv #(publish-apps %) deploy-res)] push-res)
                    (h1-error! "Compilation failed: " deploy-res))))))))))

(comment
  (run-monorepo)
  ;
)
