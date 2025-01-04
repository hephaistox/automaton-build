(ns automaton-build.tasks.doctor
  "Diagnoses common issue on the monorepo."
  (:require
   [automaton-build.code.formatter           :as build-formatter]
   [automaton-build.data.schema              :as build-data-schema]
   [automaton-build.echo.headers             :refer [h1
                                                     h1-error
                                                     h1-error!
                                                     h1-valid
                                                     h1-valid!
                                                     h2
                                                     h2-error
                                                     h2-error!
                                                     h2-valid
                                                     h2-valid!
                                                     normalln
                                                     uri-str]]
   [automaton-build.os.cli-opts              :as build-cli-opts]
   [automaton-build.os.exit-codes            :as build-exit-codes]
   [automaton-build.os.file                  :as build-file]
   [automaton-build.os.user                  :as build-user]
   [automaton-build.project.config           :as build-project-config]
   [automaton-build.project.map              :as build-project-map]
   [automaton-build.tasks.deps-version       :as tasks-deps-version]
   [automaton-build.tasks.impl.headers.cmds  :refer [blocking-cmd success]]
   [automaton-build.tasks.impl.headers.files :as    build-headers-files
                                             :refer [print-edn-errors print-file-errors]]
   [clojure.pprint                           :as pp]
   [clojure.string                           :as str]))

(def cli-opts
  (-> []
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)
      build-cli-opts/parse-cli))

(def verbose (get-in cli-opts [:options :verbose]))

(defn- doctor-cmd
  [cmd app-dir valid-msg invalid-msg]
  (let [res (blocking-cmd cmd app-dir invalid-msg verbose)]
    (if (success res)
      {:message valid-msg
       :status :ok}
      {:message invalid-msg
       :details (:out res)})))

(defn project-config
  "Log if the `project.edn` is present readable and a valid edn."
  [app-dir]
  (let [project-desc (build-headers-files/project-config app-dir)
        {:keys [filename edn]} project-desc
        defaulted-edn (build-data-schema/add-default-values build-project-config/schema edn)]
    (when-not (print-edn-errors project-desc)
      (if (build-data-schema/valid? build-project-config/schema defaulted-edn)
        {:message (str (uri-str filename) "'s file content is not a valid `edn`.")
         :status :ok}
        {:message (str (uri-str filename) " not conform to `schema`.")
         :details (build-data-schema/humanize build-project-config/schema edn)}))))

(defn npm-audit
  "Check `npm` is well installed."
  [app-dir]
  (doctor-cmd ["npm" "audit"] app-dir "npm is valid" "npm audit returns errors:"))

(defn user-id
  [app-dir]
  (doctor-cmd (build-user/user-id-cmd) app-dir "user is found" "user is not found"))

(defn group-id
  [app-dir]
  (doctor-cmd (build-user/user-id-cmd) app-dir "group is found" "group is not found"))

(defn git-installed
  "Git tooling is installed."
  [app-dir]
  (doctor-cmd ["git" "-v"] app-dir "Git is properly installed." "Don't find remote git repo."))

(defn zprint
  [app-dir]
  (doctor-cmd ["zprint" "-w" "deps.edn"]
              app-dir
              "zprint is has validated `deps.edn`"
              "zprint is not well installed."))

(defn zprint-setup
  [_app-dir]
  (let [home-setup-file-desc (build-formatter/read-home-setup)
        {:keys [raw-content]} home-setup-file-desc]
    (when-not (print-file-errors home-setup-file-desc)
      (if (build-formatter/is-zprint-using-project-setup? raw-content)
        {:message "zprint use properly project setup."
         :status :ok}
        {:message "zprint is using your local configuration."}))))

(defn docker-present
  [_app-dir]
  (doctor-cmd ["docker" "-v"] "." "docker found." "docker not found."))

(defn docker-on
  [_app-dir]
  (doctor-cmd ["docker" "run" "hello-world"] "." "docker is started." "docker is not started."))

(defn outdated-deps
  [app-dir]
  (let [project-desc (build-headers-files/project-config app-dir)
        excluded-deps (get-in project-desc [:edn :deps :excluded-libs])
        deps-report (tasks-deps-version/outdated-deps-report app-dir excluded-deps)]
    (case (:status deps-report)
      :done {:message "All dependencies are up-to-date!"
             :status :ok}
      :found {:message "You have outdated deps, run `bb update-deps`"
              :details (with-out-str (pp/print-table [:name :current-version :version]
                                                     (:deps deps-report)))}
      {:message "Dependency version check has failed"
       :details (pr-str deps-report)})))

(defn- message-version-alignments
  "Display message and return `true` if versions are matching."
  ([tool-name expected-version actual-version]
   (message-version-alignments tool-name expected-version actual-version nil))
  ([tool-name expected-version actual-version gha-image-version]
   (if (and (= expected-version actual-version)
            (or (nil? gha-image-version) (= expected-version gha-image-version)))
     (h2-valid tool-name "version" expected-version "aligned in yml, dockerfile and expectation")
     (do (h2-error tool-name "version" expected-version "not aligned")
         (normalln "Expect version" expected-version)
         (when (some? gha-image-version)
           (when-not (= expected-version gha-image-version)
             (normalln "but gha image is" gha-image-version)))
         (when-not (= expected-version actual-version)
           (normalln "but locally installed is" actual-version))))
   (and (= expected-version actual-version)
        (or (nil? gha-image-version) (= expected-version gha-image-version)))))

(defn- bb-version-check
  [versions Dockerfile]
  (h2 "Check bb version")
  (let [res (blocking-cmd ["bb" "--version"] "." "Impossible to retrieve bb version" verbose)]
    (message-version-alignments "bb"
                                (:bb versions)
                                (second (re-find #"v(.*)$" (:out res)))
                                (second (re-find #"(?m)BB_VERSION=(.*)$" Dockerfile)))))

(defn- clojure-version-check
  [versions Dockerfile]
  (h2 "Check clojure version")
  (let [res
        (blocking-cmd ["clojure" "--version"] "." "Impossible to retrieve clojure version" verbose)]
    (message-version-alignments "clojure"
                                (:clj versions)
                                (second (re-find #"version (.*)$" (:out res)))
                                (second (re-find #"(?m)CLJ_VERSION=(.*)$" Dockerfile)))))

(defn- java-version-check
  [versions Dockerfile]
  (h2-valid! "Check java version")
  (let [res (blocking-cmd ["java" "--version"] "." "Impossible to retrieve java version" verbose)]
    (normalln "java expects" (:jdk versions))
    (normalln "locally" (second (re-find #"(?m)openjdk (.*) .*$" (:out res))))
    (normalln "gha" (second (re-find #"(?m)JDK_VERSION=(.*)$" Dockerfile)))))

(defn- npm-version-check
  [versions]
  (h2 "Check npm version")
  (let [res (blocking-cmd ["npm" "--version"] "." "Impossible to retrieve npm version" verbose)]
    (message-version-alignments "npm" (:npm versions) (second (re-find #"(.*)$" (:out res))))))

(defn version
  [app-dir]
  (let [monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config)
        Dockerfile-pm (build-file/read-file "container_images/gha_image/Dockerfile")
        Dockerfile (:raw-content Dockerfile-pm)
        versions (get-in monorepo-project-map [:project-config-filedesc :edn :versions])]
    (when (:invalid? Dockerfile-pm)
      (h1-error! "gha-image dockerfile has not been found")
      (System/exit build-exit-codes/invalid-state))
    (when (:invalid? monorepo-project-map)
      (h1-error! "Monorepo project.edn has not been found")
      (System/exit build-exit-codes/invalid-state))
    (if (some some?
              (map :exit
                   [(bb-version-check versions Dockerfile)
                    (clojure-version-check versions Dockerfile)
                    (java-version-check versions Dockerfile)
                    (npm-version-check versions)]))
      {:message "Dependency version check has failed"
       :details nil}
      {:message "Dependency version is ok"
       :details nil})))

(def registry
  [{:check-name "project.edn"
    :fn-to-call project-config}
   {:check-name "docker-present"
    :fn-to-call docker-present}
   {:check-name "version"
    :fn-to-call version}
   {:check-name "docker-on"
    :fn-to-call docker-on}
   {:check-name "group-id"
    :fn-to-call group-id}
   {:check-name "user-id"
    :fn-to-call user-id}
   {:check-name "zprint"
    :fn-to-call zprint}
   {:check-name "zprint-setup"
    :fn-to-call zprint-setup}
   {:check-name "git-installed"
    :fn-to-call git-installed}
   {:check-name "npm"
    :fn-to-call npm-audit}
   {:check-name "outdated-deps"
    :fn-to-call outdated-deps}])

(defn run
  "Execute all tests."
  []
  (try (normalln "Check monorepo setup.")
       (normalln)
       (let [app-dir ""
             synthesis (->> registry
                            (mapv (fn [{:keys [check-name fn-to-call]}]
                                    (h1 "Check" check-name)
                                    (let [s (new java.io.StringWriter)
                                          {:keys [message details status]}
                                          (when (fn? fn-to-call)
                                            (binding [*out* s] (fn-to-call app-dir)))]
                                      (if (= :ok status)
                                        (h1-valid "Check" check-name)
                                        (do (h1-error "Failed check" (str check-name ":"))
                                            (normalln message)))
                                      (when-not (str/blank? details) (normalln details))
                                      (let [s (str s)] (when-not (str/blank? s) (println s)))
                                      {check-name (= :ok status)})))
                            (apply merge))]
         (normalln)
         (h1-valid! "Synthesis:")
         (doseq [[check-name check-status] synthesis]
           (if check-status (h2-valid! check-name) (h2-error! check-name))))
       (catch Exception e (println "Unexpected error during doctor:") (println (pr-str e)))))
