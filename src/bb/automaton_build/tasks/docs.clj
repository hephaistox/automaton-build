(ns automaton-build.tasks.docs
  "Publish documentation.

  Proxy to [codox](https://github.com/weavejester/codox?tab=readme-ov-file)."
  (:refer-clojure :exclude [keep])
  (:require
   [automaton-build.code.vcs                 :as build-vcs]
   [automaton-build.echo.headers             :refer [build-writter
                                                     errorln
                                                     h1-valid!
                                                     h2
                                                     h2-error
                                                     h2-error!
                                                     h2-valid
                                                     h2-valid!
                                                     normalln
                                                     print-writter
                                                     uri-str]]
   [automaton-build.html.redirect            :as build-html-redirect]
   [automaton-build.monorepo.apps            :as build-apps]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.cmds                  :as build-commands]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.filename              :as build-filename]
   [automaton-build.project.map              :as build-project-map]
   [automaton-build.tasks.impl.headers.cmds  :refer [blocking-cmd
                                                     chain-cmds
                                                     clj-parameterize
                                                     success]]
   [automaton-build.tasks.impl.headers.deps  :refer [deps-edn]]
   [automaton-build.tasks.impl.headers.files :as build-headers-files]
   [automaton-build.tasks.impl.headers.vcs   :as build-headers-vcs]))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(def ^:private cli-opts-common-def
  (-> [["-r" "--remote" "Publish remotely the documentation"]
       ["-k" "--keep" "Keep the temporary directories"]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)))

(def ^:private verbose
  (get-in (build-cli-opts/parse-cli cli-opts-common-def) [:options :verbose]))

(def ^:private keep
  (get-in (build-cli-opts/parse-cli cli-opts-common-def) [:options :keep]))

(defn- remove-if
  "Remove the `dir` content if `keep` is true."
  [dir]
  (when-not keep
    (when verbose (normalln "Remove dir" dir))
    (build-file/delete-dir dir)))

(defn- ask-user-to-check-gh-setup
  "Display a message to check the setup of the branch."
  [app-name doc-branch]
  (normalln "Check the following settings are set in github pages."
            (uri-str (format "https://github.com/hephaistox/%s/settings/pages"
                             app-name)))
  (normalln "Build and deployment > Source = \"Deploy from a branch\"")
  (normalln "Build and deployment > branch = \""
            doc-branch
            "\", with root dir."))

(defn- create-latest-symlink
  "Creates a latest symlink in the `target-dir` to `subdir`."
  [target-dir subdir]
  (let [symlink-path (build-filename/create-dir-path target-dir "latest")]
    (build-file/delete-path symlink-path)
    (-> (build-filename/create-dir-path target-dir subdir)
        (build-headers-files/create-sym-link symlink-path target-dir))))


;; ********************************************************************************
;; main steps
;; ********************************************************************************
(defn- locally-build-website
  "Builds locally the doc website for application in `app-dir`.

  The site is generated for `version`, meaning it will be displayed in the pages and the `tag` will be used to reference the code.
  Codox will generate the website in `codox-dir` as setup in the `codox` alias.

  Returns `true` if succesful and next step should go on."
  [app-dir codox-dir version]
  (when verbose
    (normalln
     "Sources link will work only after the project is build locally also."))
  (h2 "Creates website")
  (let [s (build-writter)
        {:keys [exit]}
        (binding [*out* s]
          (-> ["clojure" "-X:codox" ":version" (clj-parameterize version)]
              (blocking-cmd app-dir "Impossible to execute codox." verbose)))]
    (if (zero? exit)
      (h2-valid "Website created")
      (h2-error "Website has not been created properly. Visit output in"
                (uri-str codox-dir)))
    (print-writter s)
    (zero? exit)))

(defn- build-latest-version
  "Assemble all documentation website components:

  * `codox-dir` to get the static website generated by codox.
  * `doc-paths` and `resource-filters` to copy necessary images.

  Returns the directory of the latest version."
  [codox-dir doc-paths resource-filters]
  (let [latest-version-dir (build-file/create-temp-dir)]
    (if verbose
      (h2-valid! "Assemble codox website into" (uri-str latest-version-dir))
      (h2-valid! "Assemble codox website"))
    (build-headers-files/copy-files codox-dir latest-version-dir "*" verbose {})
    (doseq [doc-path doc-paths]
      (if verbose
        (h2-valid! "Copy the repo images from" (uri-str doc-path)
                   "with filter" (uri-str resource-filters))
        (h2-valid! "Copy the repo images"))
      (build-headers-files/copy-files doc-path
                                      latest-version-dir
                                      resource-filters
                                      verbose
                                      {}))
    latest-version-dir))

(defn- integrates-in-former-versions
  "Clone the `repo-url` to integrate the new version:

  * The modification is made in branch `branch` which is created as an empty branch based on `base-branch` if it is not existing already.
  * The `latest-version-dir` is inserted in the existing website.

  Returns the directory where the assembly has been done."
  [app-name repo-url branch base-branch latest-version-dir version]
  (let [with-all-versions-dir (build-file/create-temp-dir "publishing")]
    (h1-valid! "Integrate latest version's into the latest pushed website.")
    (if (-> (build-vcs/remote-branches-chain-cmd repo-url)
            (build-commands/force-dirs (build-file/create-temp-dir))
            build-commands/chain-cmds
            build-vcs/remote-branches
            (build-vcs/remote-branch-exists? branch))
      (do (build-headers-vcs/clone-repo-branch with-all-versions-dir
                                               repo-url
                                               branch
                                               verbose)
          (ask-user-to-check-gh-setup app-name branch))
      (do (h2-error! "Remote branch"
                     (uri-str branch)
                     "not found - creation in progress.")
          (build-headers-vcs/create-empty-branch with-all-versions-dir
                                                 repo-url
                                                 branch
                                                 base-branch
                                                 verbose)))
    (h2-valid! "Copy files from the local website"
               (when verbose (uri-str latest-version-dir)))
    (build-headers-files/copy-files latest-version-dir
                                    (-> with-all-versions-dir
                                        (build-filename/create-dir-path
                                         version))
                                    "*"
                                    verbose
                                    {:hidden false})
    (h2-valid! "Creates welcome page")
    (let [welcome-page (build-filename/create-file-path with-all-versions-dir
                                                        "index.html")]
      (build-file/delete-path welcome-page)
      (build-file/write-file welcome-page
                             (build-html-redirect/page "latest/index.html")))
    (h2-valid! "Update latest to" version)
    (create-latest-symlink with-all-versions-dir version)
    with-all-versions-dir))

(defn- push-to-remote
  "Commit the repo in `repo-dir` with message `version` into branch `branch`."
  [repo-dir repo-url branch version]
  (h1-valid! "Publish to" (uri-str repo-url)
             ", branch" (uri-str branch)
             ", version" version)
  (let [s (build-writter)
        res (binding [*out* s]
              (-> (build-vcs/commit-chain-cmd version)
                  (build-commands/force-dirs repo-dir)
                  (chain-cmds "Impossible to commit" verbose)
                  build-commands/first-failing
                  build-vcs/commit-analyze))
        {:keys [nothing-to-commit]} res]
    (if nothing-to-commit (errorln "Nothing to commit.") (print-writter s))
    (when (success res)
      (let [push-res (-> (build-vcs/push-cmd branch false)
                         (blocking-cmd repo-dir "Pushing has failed." verbose)
                         build-vcs/push-analyze)]
        (when (:nothing-to-push push-res)
          (errorln "Nothing has been pushed")
          (normalln "Visit local assemble website here:" (uri-str repo-dir)))
        (success push-res)))))


(defn- publish-doc-task
  "For application stored in `app-dir`, build and publish the doc website with version `version` in the branch `doc-branch`.

    All files (most probably images) matching `resource-filters` are copied also."
  [app-dir project-config version resource-filters doc-branch remote?]
  (let [{:keys [app-name publication]} (:edn project-config)
        {:keys [repo-url base-branch]} publication
        deps (deps-edn app-dir)
        codox-dir (->> (get-in deps [:aliases :codox :exec-args :output-path])
                       (build-filename/create-dir-path app-dir))
        doc-paths (->> (get-in deps [:aliases :codox :exec-args :doc-paths])
                       (mapv #(build-filename/create-dir-path app-dir %)))]
    (normalln "Technical documentation of project"
              (uri-str app-name)
              (when-not remote? "(run locally only)")
              ".")
    (normalln)
    (h1-valid! "Build locally the website.")
    (when (locally-build-website app-dir codox-dir version)
      (when-let [latest-version-dir
                 (-> codox-dir
                     (build-latest-version doc-paths resource-filters))]
        (remove-if codox-dir)
        (when-let [assembly-dir (integrates-in-former-versions
                                 app-name
                                 repo-url
                                 doc-branch
                                 base-branch
                                 latest-version-dir
                                 version)]
          (remove-if latest-version-dir)
          (if-not remote?
            (do (h1-valid! "Local website is successfully built in"
                           (uri-str (build-filename/create-dir-path
                                     assembly-dir
                                     "index.html")))
                (normalln "launch with -r option to push it remotely"))
            (if (push-to-remote assembly-dir repo-url doc-branch version)
              (h2-valid! "Version successfully pushed on"
                         (uri-str (format "https://hephaistox.github.io/%s"
                                          app-name)))
              (do (h2-error! "Pushing has failed")
                  (normalln "Check local version in" (uri-str assembly-dir))))))
        latest-version-dir))))

;;*****************************************************************
;; API
;;*****************************************************************
(def cli-opts
  (build-cli-opts/parse-cli ; ["automaton-build"]
   (-> cli-opts-common-def
       (conj build-apps/specify-project))))

(defn run-app
  "Entry point for running the documentation of the current application of the repl.

  * The `resource-filters` are used to find what images to copy to the website.

  Gather data from:

  * The `project-config` is used to retrieve the `app-name` and the `repo-url` and `base-branch`.
  * The  `doc-paths` is coming from the `deps.edn` codox alias.
  * Push the modifications to `doc-branch`"
  [resource-filters doc-branch]
  (let [cli-opts (build-cli-opts/parse-cli cli-opts-common-def)
        version (get-in cli-opts [:arguments 0])]
    (when verbose (normalln "Version " version))
    (publish-doc-task "."
                      (:edn (build-headers-files/project-config "."))
                      version
                      resource-filters
                      doc-branch
                      (get-in cli-opts [:options :remote]))))

(def cli-opts-monorepo
  (build-cli-opts/parse-cli (-> cli-opts-common-def
                                (conj build-apps/specify-project))))

(defn run-monorepo
  "Generates the documentation of one application in the monorepo.

  * The `resource-filters` are used to find what images to copy to the website.

  Gather data from:

  * The `project-config` is used to retrieve the `app-name` and the `repo-url` and `base-branch`.
  * The  `doc-paths` is coming from the `deps.edn` codox alias.
  * Push the modifications to `doc-branch`."
  [resource-filters doc-branch]
  (let [version (get-in cli-opts-monorepo [:arguments 0])
        app-dir ""
        monorepo-name :default
        {:keys [project]} (:options cli-opts-monorepo)
        monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config
                                 build-project-map/add-deps-edn
                                 (build-apps/add-monorepo-subprojects
                                  monorepo-name)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config
                                  build-project-map/add-deps-edn))]
    (when verbose (normalln "Version" version))
    (if-let [{:keys [app-dir project-config-filedesc]}
             (first (filter
                     (fn [subproject]
                       (and some? project (= project (:app-name subproject))))
                     (:subprojects monorepo-project-map)))]
      (publish-doc-task app-dir
                        project-config-filedesc
                        version
                        resource-filters
                        doc-branch
                        (get-in cli-opts-monorepo [:options :remote]))
      (build-headers-files/invalid-project-name-message monorepo-project-map))))

(def arguments
  {:doc-str "VERSION"
   :message "Where VERSION is the name of the version you want to push."
   :valid-fn (fn [texts]
               (and (= 1 (count texts))
                    (when-let [text (first texts)]
                      (->> text
                           (re-find #"(\d*)\.(\d*)\.(\d*)")
                           vec))))})