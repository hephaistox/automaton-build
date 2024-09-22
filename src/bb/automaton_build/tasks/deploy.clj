(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.code.vcs                 :as build-vcs]
   [automaton-build.echo.headers             :refer
                                             [h1 h1-error h1-error! h1-valid h1-valid! normalln]]
   [automaton-build.monorepo.apps            :as build-apps]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.cmds                  :as build-commands]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.filename              :as build-filename]
   [automaton-build.os.version               :as build-version]
   [automaton-build.project.compile          :as build-project-compile]
   [automaton-build.project.deps             :as build-deps]
   [automaton-build.project.map              :as build-project-map]
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
       ["-m" "--message" "Message for local apps push" :parse-fn str]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(defn- pull-base-branch
  "Pull base branch, cannot be executed on base branch"
  [app-dir base-branch current-branch]
  (if (= current-branch base-branch)
    {:status :failed
     :message
     "current monorepo branch `%s` is same as base-branch: `%s`, please switch to dev branch"}
    (let [res (-> (build-vcs/pull-changes-chain-cmd base-branch)
                  (concat [[(build-vcs/merge-cmd base-branch current-branch)]])
                  (build-commands/force-dirs app-dir)
                  build-commands/chain-cmds
                  build-commands/first-failing)]
      (if (build-commands/success res)
        {:status :success}
        {:status :failed
         :message "Pull of base branch and merge failed"
         :res res}))))

(defn clean-state?
  [app-dir]
  (if (-> (build-vcs/git-changes?-cmd)
          (build-commands/blocking-cmd app-dir)
          build-vcs/git-changes?-analyze
          not)
    {:status :success}
    {:status :failed
     :message "Git status is not empty. First commmit your changes."}))

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
  [new-git-dir repo-dir verbose?]
  (let [dir-to-push-git-dir (build-filename/create-dir-path repo-dir ".git")]
    (build-file/delete-dir dir-to-push-git-dir)
    (build-headers-files/copy-files new-git-dir dir-to-push-git-dir "*" verbose? {})))

(defn- replace-branch-files
  "Replaces files from target-branch with files from files-dir. Returns directory in which it resides"
  [files-dir repo-address target-branch verbose?]
  (let [target-git-dir (target-branch-git-dir repo-address target-branch)
        dir-with-replaced-files (build-file/create-temp-dir)]
    (when target-git-dir
      (build-headers-files/copy-files files-dir dir-with-replaced-files "*" verbose? {})
      (replace-repo-git-dir target-git-dir dir-with-replaced-files verbose?)
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
  ([source-dir repo-address target-branch commit-msg verbose?]
   (if-let [dir-to-push (replace-branch-files source-dir repo-address target-branch verbose?)]
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
  [app-dir app-name repo main-branch current-branch message verbose?]
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
                                    message)
                                  verbose?)))

(defn push-base-branch
  "Pushes app to `base-branch`"
  [app-name app-dir repo base-branch message verbose?]
  (let [github-new-changes-link (str "https://github.com/" (first (re-find #"(?<=:)(.*)(?=.git)"
                                                                           repo))
                                     "/tree/" base-branch)
        message (format "Push: `%s` to branch: `%s`. \nChanges can be seen here `%s`"
                        app-name
                        base-branch
                        github-new-changes-link)]
    (h1 message)
    (let [res (push-local-dir-to-repo app-dir repo base-branch message verbose?)]
      (if (= :success (:status res)) (h1-valid message) (h1-error "Push failed " res))
      (= :success (:status res)))))

(defn current-branch-name-invalid?
  [subapps current-branch]
  (let [base-branches
        (distinct (mapcat (fn [app]
                            [(get-in app [:project-config-filedesc :edn :publication :base-branch])
                             (get-in app [:project-config-filedesc :edn :publication :la-branch])])
                   subapps))]
    (if (some #(= current-branch %) base-branches)
      {:status :failed
       :message "Monorepo current branch is same as base branch of subapp"}
      {:status :success})))

(defn ensure-no-cache
  [new-changes-branch repo]
  (let [tmp-dir (build-file/create-temp-dir)]
    (when (build-headers-vcs/clone-repo-branch tmp-dir repo new-changes-branch false) tmp-dir)))

(defn- wrap-fn-ex
  [f]
  (try (f)
       (catch Exception e
         {:status :failed
          :ex e})))

(defn compile*
  [app-dir
   app-name
   deps-edn
   excluded-aliases
   shadow-deploy-alias
   css-files
   compiled-css-path
   compile-jar
   compile-uber-jar
   jar-entrypoint
   java-opts
   env]
  (let [paths (mapv #(build-filename/absolutize (build-filename/create-dir-path app-dir %))
                    (build-deps/extract-paths deps-edn excluded-aliases))
        class-dir (build-filename/absolutize
                   (build-filename/create-dir-path app-dir (format "target/%s/class/" (name env))))
        target-jar-filename (build-filename/create-file-path
                             (format "target/%s/%s.jar" (name env) app-name))
        shadow-res (if shadow-deploy-alias
                     (let [res (wrap-fn-ex (partial build-project-compile/shadow-cljs
                                                    app-dir
                                                    shadow-deploy-alias))]
                       (if (build-commands/success res)
                         {:status :success}
                         {:status :failed
                          :res res}))
                     {:status :skipped})
        css-res (if (and css-files compiled-css-path)
                  (let [res
                        (wrap-fn-ex
                         (partial build-project-compile/css app-dir css-files compiled-css-path))]
                    (if (build-commands/success res)
                      {:status :success}
                      {:status :failed
                       :res res}))
                  {:status :skipped})
        jar-res
        (if compile-jar
          (wrap-fn-ex
           (partial build-project-compile/compile-jar class-dir paths target-jar-filename app-dir))
          {:status :skipped})
        uber-jar-res (if compile-uber-jar
                       (wrap-fn-ex (partial build-project-compile/compile-uber-jar
                                            class-dir
                                            paths
                                            target-jar-filename
                                            app-dir
                                            jar-entrypoint
                                            java-opts))
                       {:status :skipped})]
    (-> {}
        (assoc :app-dir app-dir)
        (assoc :paths paths)
        (assoc :class-dir class-dir)
        (assoc :shadow-cljs shadow-res)
        (assoc :css css-res)
        (assoc :jar jar-res)
        (assoc :uber-jar uber-jar-res)
        (assoc :status :success))))

(defn publish-clever-cloud
  [clever-uri app-dir env verbose?]
  (build-project-publish/publish-clever-cloud clever-uri
                                              (->> (name env)
                                                   (build-filename/create-dir-path app-dir "target")
                                                   build-filename/absolutize)
                                              (->> ".clever"
                                                   (build-filename/create-dir-path app-dir)
                                                   build-filename/absolutize)
                                              (build-version/current-version app-dir)
                                              verbose?))

(defn compile-app
  "Deploys app in isolation to ensure no cache which requires that your state should be clean and current branch is not a base branch

   The process of deployment is:
   1. Compile jar
   2. Push changes to app target branch for that environment
   3. Deploy app

  It's done currently not as a workflow, only because workflow can't be used in another workflow and we have a logic we want to preserve here"
  [dir
   app-name
   deps-edn
   excluded-aliases
   shadow-deploy-alias
   css-files
   compiled-css-path
   compile-jar
   compile-uber-jar
   jar-entrypoint
   java-opts
   env
   current-branch
   repo
   target-branch]
  (if (and repo
           target-branch
           (build-project-versioning/correct-environment? dir env)
           (build-project-versioning/version-changed? dir repo target-branch))
    (if-let [app-dir (ensure-no-cache current-branch repo)]
      (compile* app-dir
                app-name
                deps-edn
                excluded-aliases
                shadow-deploy-alias
                css-files
                compiled-css-path
                compile-jar
                compile-uber-jar
                jar-entrypoint
                java-opts
                env)
      {:status :failed
       :msg "Couldn't ensure that there is no cache"})
    {:status :skipped
     :data {:repo repo
            :target-branch target-branch
            :env env}
     :msg (if (and repo target-branch) "No changes found" "Missing parameters")}))

(defn publish-app
  [{:keys [app-name
           app-dir
           repo
           target-branch
           status
           publish-clojars?
           publish-cc?
           jar
           env
           cc-uri
           paths
           as-lib
           pom-xml-license]
    :as app}
   verbose?]
  (if (= :success status)
    (if (push-base-branch app-name
                          app-dir
                          repo
                          target-branch
                          (build-version/current-version app-dir)
                          verbose?)
      (let [publish-cc (-> (if publish-cc?
                               (publish-clever-cloud cc-uri app-dir env verbose?)
                               {:status :skipped
                                :message ":publication :cc project.edn is missing"})
                           (assoc :publish :cc))
            publish-clojars (-> (if publish-clojars?
                                    (build-project-publish/publish-clojars (:jar-path jar)
                                                                           app-dir
                                                                           paths
                                                                           as-lib
                                                                           pom-xml-license
                                                                           verbose?)
                                    {:status :skipped
                                     :message ":publication :clojars project.edn is missing"})
                                (assoc :publish :clojars))]
        (if (every? (fn [{:keys [status]}] (= status :skipped)) [publish-cc publish-clojars])
          {:status :skipped
           :message (str "clever cloud skipped: " (:message publish-cc)
                         " | clojars skipped: " (:message publish-clojars))}
          (if (every? (fn [{:keys [status]}] (or (= status :success) (= status :skipped)))
                      [publish-cc publish-clojars])
            {:status :success
             :res (filter #(= :success (:status %)) [publish-cc publish-clojars])}
            {:status :failed
             :res [publish-cc publish-clojars]})))
      {:status :failed
       :msg (str "Push to " target-branch " failed")})
    {:status :skipped
     :message (str "because compilation status is: " status " more details: " (:msg app))}))

(defn compile-monorepo
  [{:keys [app-name app-dir]
    :as app}
   target-branch-env
   clever-uri-env
   env
   current-branch
   verbose?]
  (h1 app-name " being compiled")
  (let [project-config (get-in app [:project-config-filedesc :edn])
        repo (get-in project-config [:publication :repo-url])
        base-branch (get-in project-config [:publication target-branch-env])
        cc-uri (get-in project-config [:publication clever-uri-env])
        publish-clojars? (get-in project-config [:publication :clojars])
        publish-cc? (get-in project-config [:publication :cc])
        as-lib (get-in project-config [:publication :as-lib])
        pom-xml-license (get-in project-config [:publication :pom-xml-license])
        excluded-aliases (get-in project-config [:publication :excluded-aliases])
        deps-edn (get-in app [:deps :edn])
        shadow-deploy-alias (get-in project-config [:publication :shadow-cljs-deploy-alias])
        css-files (get-in project-config [:publication :css-files])
        compiled-css-path (get-in project-config [:publication :compiled-css-path])
        compile-jar (get-in project-config [:publication :compile-jar])
        compile-uber-jar (get-in project-config [:publication :compile-uber-jar])
        jar-entrypoint (get-in project-config [:publication :uber-jar :entrypoint])
        java-opts (get-in project-config [:publication :uber-jar :java-opts])
        compile-res (-> (compile-app app-dir
                                     app-name
                                     deps-edn
                                     excluded-aliases
                                     shadow-deploy-alias
                                     css-files
                                     compiled-css-path
                                     compile-jar
                                     compile-uber-jar
                                     jar-entrypoint
                                     java-opts
                                     env
                                     current-branch
                                     repo
                                     base-branch)
                        (assoc :cc-uri cc-uri)
                        (assoc :env env)
                        (assoc :publish-clojars? publish-clojars?)
                        (assoc :publish-cc? publish-cc?)
                        (assoc :repo repo)
                        (assoc :target-branch base-branch)
                        (assoc :app-name app-name)
                        (assoc :as-lib as-lib)
                        (assoc :pom-xml-license pom-xml-license))]
    (if (every? #(or (= :success (:status %)) (= :skipped (:status %)))
                (concat (vals (select-keys compile-res [:shadow-cljs :css :jar :uber-jar]))
                        [{:status (:status compile-res)}]))
      (do (h1-valid app-name " compiled")
          (h1 app-name " deploying")
          (let [res (-> compile-res
                        (publish-app verbose?)
                        (assoc :app-name app-name))]
            (cond
              (= :success (:status res)) (h1-valid app-name " deployed")
              (= :skipped (:status res)) (normalln app-name " deployment skipped")
              :else (h1-error app-name " failed deployment with: " res))
            res))
      (do (h1-error app-name " failed compilation with: " compile-res)
          {:status :failed
           :res compile-res}))))

(defn run-monorepo
  []
  (try
    (let [monorepo-project-map (-> (build-project-map/create-project-map "")
                                   build-project-map/add-project-config
                                   (build-apps/add-monorepo-subprojects :default)
                                   (build-apps/apply-to-subprojects
                                    build-project-map/add-deps-edn
                                    build-project-map/add-project-config))
          env (get-in cli-opts [:options :env])
          message (get-in cli-opts [:options :message])
          verbose? (get-in cli-opts [:options :verbose])
          base-branch (get-in monorepo-project-map
                              [:project-config-filedesc :edn :publication :base-branch])
          app-dir (get-in monorepo-project-map [:app-dir])
          current-branch (build-headers-vcs/current-branch app-dir)
          subapps (->> monorepo-project-map
                       :subprojects)]
      (if-let [failed-res (some (fn [res] (if (not= :success (:status res)) res false))
                                [(pull-base-branch app-dir base-branch current-branch)
                                 (clean-state? app-dir)
                                 (current-branch-name-invalid? subapps current-branch)])]
        (do (h1-error! "Can't deploy because " failed-res) 1)
        (let [_ (h1 "Pushing all monorepo apps to " current-branch)
              push-current-branch-subapps
              (mapv (fn [{:keys [app-dir app-name]
                          :as app}]
                      (h1 app-name " being pushed to " current-branch)
                      (let [app-base-branch
                            (get-in app [:project-config-filedesc :edn :publication :base-branch])
                            repo (get-in app [:project-config-filedesc :edn :publication :repo-url])
                            push-res (push-current-branch app-dir
                                                          app-name
                                                          repo
                                                          app-base-branch
                                                          current-branch
                                                          message
                                                          verbose?)]
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
                  push-res (reduce (fn [acc app]
                                     (if (some (fn [{:keys [status]}] (= :failed status)) acc)
                                       (conj acc
                                             {:app-name (:app-name app)
                                              :status :skipped
                                              :message "previous app failed"})
                                       (conj acc
                                             (compile-monorepo app
                                                               target-branch-env
                                                               clever-uri-env
                                                               env
                                                               current-branch
                                                               verbose?))))
                                   []
                                   subapps)]
              (mapv (fn [res]
                      (cond
                        (= :success (:status res)) (apply h1-valid!
                                                          (:app-name res)
                                                          " successfully deployed to "
                                                          (mapcat #(str (name (:publish %)))
                                                           (filter #(= :success (:status %))
                                                                   (:res res))))
                        (= :skipped (:status res))
                        (normalln (:app-name res) " skipped due to " (:message res))
                        :else (h1-error! (:app-name res) " failed with: " res)))
                    push-res))))))
    0
    (catch Exception e (h1-error! "error happened: " e) 1)))
