(ns automaton-build.tasks.4
  "Prepare project for proper commit."
  (:refer-clojure :exclude [format])
  (:require
   [automaton-build.code.cljs                 :as build-cljs]
   [automaton-build.code.formatter            :as build-formatter]
   [automaton-build.echo.headers              :refer [build-writter
                                                      errorln
                                                      h1
                                                      h1-error
                                                      h1-error!
                                                      h1-valid
                                                      h1-valid!
                                                      h2
                                                      h2-error
                                                      h2-valid
                                                      normalln
                                                      print-writter]]
   [automaton-build.monorepo.apps             :as build-apps]
   [automaton-build.os.cli-opts               :as build-cli-opts]
   [automaton-build.project.map               :as build-project-map]
   [automaton-build.tasks.impl.commit         :as build-tasks-commit]
   [automaton-build.tasks.impl.headers.cmds   :refer [blocking-cmd success]]
   [automaton-build.tasks.impl.headers.shadow :as build-headers-shadow]
   [automaton-build.tasks.impl.linter         :as build-linter]
   [automaton-build.tasks.impl.report-aliases :as build-tasks-report-aliases]
   [automaton-build.tasks.impl.reports-fw     :as build-tasks-reports-fw]))

;; ********************************************************************************
;; Task parameters
;; ********************************************************************************
(def cli-opts-data
  (-> [["-c" "--commit-message COMMIT-MESSAGE" "Commit message"]]
      (concat build-cli-opts/help-options build-cli-opts/verbose-options)))

(def cli-opts (build-cli-opts/parse-cli cli-opts-data))

(def verbose (get-in cli-opts [:options :verbose]))

;; ********************************************************************************
;; Helpers
;; ********************************************************************************
(defn- npm-install
  [project-dir]
  (h2 "cljs dependencies installation.")
  (let [s (build-writter)
        install-res (binding [*out* s]
                      (-> (build-cljs/install-cmd)
                          (blocking-cmd project-dir "" verbose)))
        install-success (success install-res)]
    (if install-success
      (h2-valid "npm install ok")
      (h2-error "npm install has failed"))
    (print-writter s)
    (when verbose (normalln (:out install-res)))
    install-success))

(defn- cljs-compilation
  [project-dir]
  (h2 "cljs compilation")
  (let [s (build-writter)
        compile-shadow-res (binding [*out* s]
                             (some-> (build-headers-shadow/read-dir project-dir)
                                     build-headers-shadow/build
                                     build-cljs/cljs-compile-cmd
                                     (blocking-cmd project-dir "" verbose)))
        compilation-success (success compile-shadow-res)]
    (if compilation-success
      (h2-valid "cljs compilation ok")
      (h2-error "cljs compilation has failed"))
    (print-writter s)
    (when verbose (normalln (:out compile-shadow-res)))
    compilation-success))

(defn- karma-test
  [project-dir]
  (h2 "karma test")
  (let [s (build-writter)
        cljs-test-res (binding [*out* s]
                        (-> (build-cljs/karma-test-cmd)
                            (blocking-cmd project-dir "" verbose)))
        cljs-test-success (success cljs-test-res)]
    (if cljs-test-success
      (h2-valid "karma test ok")
      (h2-error "karma has failed"))
    (print-writter s)
    (when verbose (normalln (:out cljs-test-res)))
    cljs-test-success))

;; ********************************************************************************
;; API
;; ********************************************************************************
(defn clj-test
  "Run clj test."
  [project-dir test-aliases]
  (let [s (build-writter)]
    (h1 "Test clj")
    (let [clj-res (binding [*out* s]
                    (->
                      ["clojure" (apply str "-M" test-aliases)]
                      (blocking-cmd project-dir "Error during tests" verbose)))
          clj-success (success clj-res)]
      (if-not (success clj-res)
        (errorln "clj tests have failed.")
        (h1-valid "clj test ok."))
      (print-writter s)
      (when verbose (normalln (:out clj-res)))
      clj-success)))

(defn cljs-test
  "Run cljs test"
  [project-dir]
  (h1-valid! "Test cljs")
  (let [install-success (npm-install project-dir)
        cljs-compilation-success (when install-success
                                   (cljs-compilation project-dir))
        karma-success (when cljs-compilation-success (karma-test project-dir))]
    (and install-success cljs-compilation-success karma-success)))

(defn format-files
  "Format project files."
  [project-dir]
  (h1 "Format project files.")
  (let [s (build-writter)
        res (binding [*out* s]
              (-> (build-formatter/format-clj-cmd)
                  (blocking-cmd project-dir "" verbose)))]
    (if (success res)
      (h1-valid "Format project files.")
      (h1-error "Format project files."))
    (print-writter s)
    (when verbose (normalln (:out res)))
    (success res)))

(defn run-monorepo
  [test-aliases]
  (normalln "Create a mergeable commit.")
  (normalln)
  (let [app-dir ""
        monorepo-name :default
        commit-msg (get-in cli-opts [:options :commit-message])
        monorepo-project-map (-> (build-project-map/create-project-map app-dir)
                                 build-project-map/add-project-config
                                 build-project-map/add-deps-edn
                                 (build-apps/add-monorepo-subprojects
                                  monorepo-name)
                                 (build-apps/apply-to-subprojects
                                  build-project-map/add-project-config
                                  build-project-map/add-deps-edn))
        format-status (format-files app-dir)
        report-aliases-status
        (build-tasks-report-aliases/scan-alias monorepo-project-map verbose)
        fw-status (build-tasks-reports-fw/report-monorepo monorepo-project-map)
        linter-status (build-linter/lint (:deps monorepo-project-map) verbose)
        clj-status (clj-test app-dir test-aliases)
        cljs-status (cljs-test app-dir)
        commit-status (when commit-msg
                        (build-tasks-commit/commit
                         (:dir monorepo-project-map)
                         (str commit-msg
                              (if (->> [format-status
                                        report-aliases-status
                                        fw-status
                                        linter-status
                                        clj-status
                                        cljs-status]
                                       (remove nil?)
                                       (every? true?))
                                "- wf-4 OK"
                                "- wf-4 KO"))
                         verbose))]
    (normalln)
    (normalln)
    (h1-valid! "Synthesis:")
    (if format-status
      (h1-valid! "Formatting is ok.")
      (h1-error! "Formatting has failed."))
    (build-tasks-report-aliases/synthesis report-aliases-status)
    (build-tasks-reports-fw/synthesis fw-status)
    (build-linter/synthesis linter-status)
    (if clj-status
      (h1-valid! "Clj test ok.")
      (h1-error! "Clj test has failed."))
    (if cljs-status
      (h1-valid! "Cljs test ok.")
      (h1-error! "Cljs test has failed."))
    (and format-status
         report-aliases-status
         fw-status
         linter-status
         clj-status
         cljs-status
         commit-status)))
