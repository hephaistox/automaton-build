(ns automaton-build.tasks.deploy
  (:require
   [automaton-build.code.vcs                 :as build-vcs]
   [automaton-build.echo.headers             :refer [clear-lines
                                                     clear-prev-line
                                                     h1
                                                     h1-error!
                                                     h1-valid
                                                     h1-valid!
                                                     h2
                                                     h2-error
                                                     h2-valid
                                                     h3
                                                     h3-error
                                                     h3-valid
                                                     normalln]]
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
       ["-m" "--message" "Message for local apps push" :parse-fn str]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

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

(defn- clean-state?
  [app-dir]
  (if (-> (build-vcs/git-changes?-cmd)
          (build-commands/blocking-cmd app-dir)
          build-vcs/git-changes?-analyze
          not)
    {:status :success}
    {:status :failed
     :message "Git status is not empty. First commmit your changes."}))

(defn- push-monorepo-changes-to-subapps
  [current-branch subapps message verbose?]
  (h1 "Pushing all monorepo apps to " current-branch)
  (let [res (mapv (fn [{:keys [app-dir app-name]
                        :as app}]
                    (h2 app-name " being pushed to " current-branch)
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
                        (h2-valid app-name "pushed")
                        (h2-error app-name "push failed with: " push-res))
                      push-res))
                  subapps)]
    (if (every? #(= :success (:status %)) res)
      (do (clear-lines (count subapps))
          (h1-valid "Pushed all monorepo apps to " current-branch)
          {:status :success})
      res)))



(defn push-base-branch
  "Pushes app to `base-branch`"
  [app-name app-dir repo base-branch message verbose?]
  (let [github-new-changes-link (str "https://github.com/" (first (re-find #"(?<=:)(.*)(?=.git)"
                                                                           repo))
                                     "/tree/" base-branch)
        message (format "Push: `%s` to branch: `%s`. Changes can be seen here `%s`"
                        app-name
                        base-branch
                        github-new-changes-link)]
    (h3 message)
    (let [res (push-local-dir-to-repo app-dir repo base-branch message verbose?)]
      (if (= :success (:status res)) (h3-valid message) (h3-error "Push failed " res))
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

(defn- ensure-no-cache
  [new-changes-branch repo]
  (let [tmp-dir (build-file/create-temp-dir)]
    (when (build-headers-vcs/clone-repo-branch tmp-dir repo new-changes-branch false) tmp-dir)))


(defn pom-xml-status
  [app-dir as-lib pom-xml-license paths]
  (if (and app-dir as-lib pom-xml-license)
    (build-project-pom-xml/generate-pom-xml as-lib paths app-dir pom-xml-license)
    {:status :failed
     :app-dir app-dir
     :as-lib as-lib
     :pom-xml-license pom-xml-license
     :msg "Missing required-params"}))

(defn generate-pom-xml
  [app-dir as-lib pom-xml-license paths verbose?]
  (let [_ (normalln "pom-xml generation")
        pom-xml-status (pom-xml-status app-dir as-lib pom-xml-license paths)]
    (when (and verbose? (:msg pom-xml-status) (not (str/blank? (:msg pom-xml-status))))
      (normalln (:msg pom-xml-status)))
    pom-xml-status))

(defn deploy-cc
  [{:keys [cacheless-app-dir
           deps-edn
           excluded-aliases
           shadow-deploy-alias
           css-files
           compiled-css-path
           clever-uri
           jar-entrypoint
           target-jar
           class-dir
           java-opts]
    :as _app}
   env
   verbose?]
  (let [paths (build-deps/extract-paths deps-edn excluded-aliases)
        class-dir (build-filename/absolutize (build-filename/create-dir-path cacheless-app-dir
                                                                             class-dir))
        target-jar-filename (build-filename/create-file-path target-jar)
        shadow-res (if shadow-deploy-alias
                     (build-project-compile/shadow-cljs cacheless-app-dir shadow-deploy-alias)
                     {:status :skipped})
        css-res (if (and css-files compiled-css-path)
                  (build-project-compile/css cacheless-app-dir css-files compiled-css-path)
                  {:status :skipped})
        uber-jar-res (build-project-compile/compile-uber-jar class-dir
                                                             paths
                                                             target-jar-filename
                                                             cacheless-app-dir
                                                             jar-entrypoint
                                                             java-opts)]
    (if-let [failed-res (some (fn [res] (when (= :failed (:status res)) res))
                              [shadow-res css-res uber-jar-res])]
      {:status :failed
       :res failed-res}
      ;;TODO can we get rid of this env?
      (build-project-publish/publish-clever-cloud
       clever-uri
       (->> (name env)
            (build-filename/create-dir-path cacheless-app-dir "target")
            build-filename/absolutize)
       (->> ".clever"
            (build-filename/create-dir-path cacheless-app-dir)
            build-filename/absolutize)
       (build-version/current-version cacheless-app-dir)
       verbose?))))


(defn deploy-clojars
  [{:keys [cacheless-app-dir
           as-lib
           pom-xml-license
           deps-edn
           excluded-aliases
           shadow-deploy-alias
           css-files
           compiled-css-path
           class-dir
           target-jar]
    :as _app}
   verbose?]
  (let [paths (build-deps/extract-paths deps-edn excluded-aliases)
        class-dir (build-filename/absolutize (build-filename/create-dir-path cacheless-app-dir
                                                                             class-dir))
        target-jar-filename (build-filename/create-file-path target-jar)
        pom-xml-res (generate-pom-xml cacheless-app-dir as-lib pom-xml-license paths verbose?)
        shadow-res (if shadow-deploy-alias
                     (build-project-compile/shadow-cljs cacheless-app-dir shadow-deploy-alias)
                     {:status :skipped})
        css-res (if (and css-files compiled-css-path)
                  (build-project-compile/css cacheless-app-dir css-files compiled-css-path)
                  {:status :skipped})
        jar-res
        (build-project-compile/compile-jar class-dir paths target-jar-filename cacheless-app-dir)]
    (if-let [failed-res (some (fn [res] (when (= :failed (:status res)) res))
                              [pom-xml-res shadow-res css-res jar-res])]
      {:status :failed
       :res failed-res}
      (build-project-publish/publish-clojars (:jar-path jar-res) cacheless-app-dir))))


(defn deploy-app
  [{:keys [publish-cc? publish-clojars? app-name cacheless-app-dir repo base-branch]
    :as app}
   env
   verbose?]
  (h2 app-name " deployment process started")
  (if (push-base-branch app-name
                        cacheless-app-dir
                        repo
                        base-branch
                        (build-version/current-version cacheless-app-dir)
                        verbose?)
    (let [cc (if publish-cc?
               (assoc (deploy-cc app env verbose?) :name "clever cloud")
               {:name "clever cloud"
                :message ":publication :cc project.edn is missing"
                :status :skipped})
          clojars (if publish-clojars?
                    (assoc (deploy-clojars app verbose?) :name "clojars")
                    {:name "clojars"
                     :message ":publication :clojars project.edn is missing"
                     :status :skipped})]
      (if-let [failed-res (some (fn [{:keys [status]
                                      :as res}]
                                  (when (= status :failed) res))
                                [cc clojars])]
        (do (h2-error app-name " deployment failed:" failed-res)
            {:status :failed
             :res failed-res})
        (do (h2-valid app-name " deployment suceeded")
            {:status :success
             :res [cc clojars]})))
    {:status :failed
     :msg (str "Push to " base-branch " failed")}))


(defn should-deploy?
  [app env]
  ;;TODO correct-environment check doesn't fill right here
  ;;But have in mind moving it to inital check doesn't make sense as here we decide if it should be deployed or not
  ;;Or maybe we should not deploy in that case? To think about, what happens if we don't want to deploy automaton-web and it's version is differnet than our env? Maybe this check only makes sense when there was a change?
  ;;
  ;; TODO cacheless-app-dir, is not really deploy? false, but more deploy failed.. it's technical issue if we can't create temp directory
  (let [target-branch (:base-branch app)
        repo (:repo app)
        dir (:initial-app-dir app)
        publish-clojars? (:publish-clojars? app)
        publish-cc? (:publish-cc? app)]
    (cond
      (or (nil? repo) (nil? target-branch)) {:deploy? false
                                             :reason "Missing parameters"
                                             :data {:repo repo
                                                    :target-branch target-branch
                                                    :env env}}
      (and (nil? publish-clojars?) (nil? publish-cc?)) {:deploy? false
                                                        :reason "Deployment target missing"
                                                        :data {:publish-clojars? publish-clojars?
                                                               :publish-cc? publish-cc?}}
      (nil? (:cacheless-app-dir app)) {:deploy? false
                                       :reason "Couldn't ensure that there is no cache"}
      (not (build-project-versioning/correct-environment? dir env))
      {:deploy? false
       :reason "version.edn of app is not aligned with chosen environment"}
      (not (build-project-versioning/version-changed? dir repo target-branch))
      {:deploy? false
       :reason "version.edn of app did not change"}
      :else {:deploy? true})))

(defn prepare-deploy-data
  [app current-branch env target-branch-env clever-uri-env verbose?]
  (h2 (str (:app-name app) "..."))
  (let [app-name (:app-name app)
        project-config (get-in app [:project-config-filedesc :edn])
        repo (get-in project-config [:publication :repo-url])
        app-deploy-data
        {:app-name app-name
         :repo repo
         :class-dir (format "target/%s/class/" (name env))
         :target-jar (format "target/%s/%s.jar" (name env) (:app-name app))
         :cacheless-app-dir (ensure-no-cache current-branch repo)
         :initial-app-dir (:app-dir app)
         :base-branch (get-in project-config [:publication target-branch-env])
         :clever-uri (get-in project-config [:publication clever-uri-env])
         :publish-clojars? (get-in project-config [:publication :clojars])
         :publish-cc? (get-in project-config [:publication :cc])
         :as-lib (get-in project-config [:publication :as-lib])
         :pom-xml-license (get-in project-config [:publication :pom-xml-license])
         :excluded-aliases (get-in project-config [:publication :excluded-aliases])
         :deps-edn (get-in app [:deps :edn])
         :shadow-deploy-alias (get-in project-config [:publication :shadow-cljs-deploy-alias])
         :css-files (get-in project-config [:publication :css-files])
         :compiled-css-path (get-in project-config [:publication :compiled-css-path])
         :compile-jar (get-in project-config [:publication :compile-jar])
         :compile-uber-jar (get-in project-config [:publication :compile-uber-jar])
         :jar-entrypoint (get-in project-config [:publication :uber-jar :entrypoint])
         :java-opts (get-in project-config [:publication :uber-jar :java-opts])}
        deploy?-res (should-deploy? app-deploy-data env)]
    (if (:deploy? deploy?-res)
      (do (h2-valid app-name "will be deployed") app-deploy-data)
      (do (print clear-prev-line) (normalln app-name "is skipped " (when verbose? deploy?-res))))))

(defn deploy-monorepo-apps
  "If one app failes in the deploy chain, rest of apps is skipped"
  [env verbose? apps-to-deploy]
  (let [deploy-res (reduce (fn [acc app]
                             (if (some (fn [{:keys [status]}] (= :failed status)) acc)
                               (conj acc
                                     {:app-name (:app-name app)
                                      :status :skipped
                                      :message "previous app failed"})
                               (conj acc (deploy-app app env verbose?))))
                           []
                           apps-to-deploy)]
    deploy-res))

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
          target-branch-env (cond
                              (= :la env) :la-branch
                              (= :production env) :base-branch
                              :else :imnothere)
          clever-uri-env (cond
                           (= :la env) :cc-uri-la
                           (= :production env) :cc-uri-production
                           :else :imnotthere)
          app-dir (get-in monorepo-project-map [:app-dir])
          current-branch (build-headers-vcs/current-branch app-dir)
          subapps (->> monorepo-project-map
                       :subprojects)]
      (if-let [failed-res
               (some (fn [res] (if (not= :success (:status res)) res false))
                     (list
                      (pull-base-branch app-dir base-branch current-branch)
                      (clean-state? app-dir)
                      (current-branch-name-invalid? subapps current-branch)
                      (push-monorepo-changes-to-subapps current-branch subapps message verbose?)))]
        (do (h1-error! "Can't deploy because " failed-res) 1)
        (do (h1 "Analyzing apps that should be deployed...")
            (let [apps-to-deploy (->> subapps
                                      (mapv #(prepare-deploy-data %
                                                                  current-branch
                                                                  env
                                                                  target-branch-env
                                                                  clever-uri-env
                                                                  verbose?))
                                      (remove nil?))
                  _ (h1 "Deployment of chosen apps")
                  deploy-res (deploy-monorepo-apps env verbose? apps-to-deploy)]
              (normalln)
              (h1 "Synthesis: ")
              (normalln)
              (remove nil?
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
                            deploy-res))))))
    0
    (catch Exception e (h1-error! "error happened: " e) 1)))
