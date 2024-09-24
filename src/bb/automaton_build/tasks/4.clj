(ns automaton-build.tasks.4
  "Prepare project for proper commit."
  (:refer-clojure :exclude [format])
  (:require
   [automaton-build.code.cljs                     :as build-cljs]
   [automaton-build.code.formatter                :as build-formatter]
   [automaton-build.code.vcs                      :as build-vcs]
   [automaton-build.echo.headers                  :refer [build-writter
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
   [automaton-build.monorepo.apps                 :as build-apps]
   [automaton-build.os.cli-opts                   :as build-cli-opts]
   [automaton-build.os.exit-codes                 :as build-exit-codes]
   [automaton-build.project.map                   :as build-project-map]
   [automaton-build.tasks.generate-monorepo-files :as generate-monorepo-files]
   [automaton-build.tasks.impl.commit             :as build-tasks-commit]
   [automaton-build.tasks.impl.headers.cmds       :refer [blocking-cmd success]]
   [automaton-build.tasks.impl.headers.shadow     :as build-headers-shadow]
   [automaton-build.tasks.impl.linter             :as build-linter]
   [automaton-build.tasks.impl.report-aliases     :as build-tasks-report-aliases]
   [automaton-build.tasks.impl.reports-fw         :as build-tasks-reports-fw]))

;; ********************************************************************************
;; Task parameters
;; ********************************************************************************
(def cli-opts-data
  (-> [["-c" "--commit-message COMMIT-MESSAGE" "Commit message"]
       ["-f" "--tests-frontend" "Do not execute frontend tests" :default true :parse-fn not]
       ["-d"
        "--deps-alias ALIAS"
        "Alias from deps-edn to"
        :multi
        true
        :default
        []
        :update-fn
        (fn [opt arg] (conj opt (keyword arg)))]
       ["-b" "--tests-backend" "Do not execute frontend tests" :default true :parse-fn not]
       ["-g" "--generate-files" "Do not execute generation of files" :default false :parse-fn not]
       ["-p" "--pretty-format" "Do not execute formatting" :default true :parse-fn not]
       ["-a" "--aliases" "Do not execute aliases check" :default true :parse-fn not]
       ["-w" "--words-forbidden" "Do not execute forbidden words check" :default true :parse-fn not]
       ["-l" "--linter" "Do not execute linting" :default true :parse-fn not]]
      (concat build-cli-opts/help-options
              build-cli-opts/verbose-options
              build-cli-opts/inverse-options)))

(def cli-opts
  (-> (build-cli-opts/parse-cli cli-opts-data)
      (build-cli-opts/inverse
       [:pretty-format :aliases :words-forbidden :linter :tests-frontend :tests-backend])))

(def verbose (get-in cli-opts [:options :verbose]))
(def formatting? (get-in cli-opts [:options :pretty-format]))
(def fw? (get-in cli-opts [:options :words-forbidden]))
(def aliases? (get-in cli-opts [:options :aliases]))
(def linter? (get-in cli-opts [:options :linter]))
(def tests-b? (get-in cli-opts [:options :tests-backend]))
(def tests-f? (get-in cli-opts [:options :tests-frontend]))
(def commit-msg (get-in cli-opts [:options :commit-message]))
(def deps-alias (get-in cli-opts [:options :deps-alias]))

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
    (if install-success (h2-valid "npm install ok") (h2-error "npm install has failed"))
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
    (if cljs-test-success (h2-valid "karma test ok") (h2-error "karma has failed"))
    (print-writter s)
    (when verbose (normalln (:out cljs-test-res)))
    cljs-test-success))

;; ********************************************************************************
;; API
;; ********************************************************************************
(defn clean-state
  "Check if the state is clean"
  [project-dir]
  (h1 "Check clean state.")
  (let [s (build-writter)
        res (binding [*out* s]
              (-> (build-vcs/clean-state)
                  (blocking-cmd project-dir "" verbose)))]
    (if (build-vcs/clean-state-analyze res)
      (h1-valid "git state is clean.")
      (h1-error "git state is not clean."))
    (print-writter s)
    (when verbose (normalln (:out res)))
    (build-vcs/clean-state-analyze res)))

(defn clj-test
  "Run clj test."
  [project-dir test-aliases]
  (h1 "Test clj")
  (let [s (build-writter)
        clj-res (map #(let [res (binding [*out* s]
                                  (-> ["clojure" (str "-M" %)]
                                      (blocking-cmd project-dir "Error during tests" verbose)))]
                        (when verbose (normalln (:out res)))
                        res)
                     test-aliases)
        clj-success (every? success clj-res)]
    (if-not clj-success
      (do (errorln "clj test have failed.") (print-writter s))
      (h1-valid "clj test ok."))
    clj-success))

(defn cljs-test
  "Run cljs test"
  [project-dir]
  (h1-valid! "Test cljs")
  (let [install-success (npm-install project-dir)
        cljs-compilation-success (when install-success (cljs-compilation project-dir))
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
    (if (success res) (h1-valid "Format project files.") (h1-error "Format project files."))
    (print-writter s)
    (when verbose (normalln (:out res)))
    (success res)))

(defn commit
  [app-dir success?]
  (build-tasks-commit/commit app-dir
                             (if success? (str commit-msg "- wf-4 OK") (str commit-msg "- wf-4 KO"))
                             verbose))

(defn run*
  [project-map test-aliases]
  (normalln "Create a mergeable commit.")
  (normalln)
  (let [app-dir (:app-dir project-map)
        test-aliases (if (and deps-alias (not-empty deps-alias)) deps-alias test-aliases)
        status-map (merge
                    (when fw? {:forbidden-words-check (build-tasks-reports-fw/report project-map)})
                    (when formatting? {:formatting (format-files app-dir)})
                    (when aliases?
                      {:aliases-check (build-tasks-report-aliases/scan-alias project-map verbose)})
                    (when linter? {:linting (build-linter/lint (:deps project-map) verbose)})
                    (when tests-b? {:clj-tests-check (clj-test app-dir test-aliases)})
                    (when tests-f? {:cljs-tests-check (cljs-test app-dir)})
                    {:clean-state (clean-state app-dir)})
        status (->> status-map
                    vals
                    (every? true?))]
    (normalln)
    (normalln)
    (when commit-msg (commit app-dir status))
    (if (true? status)
      build-exit-codes/ok
      (do (h1 "Synthesis:")
          (doseq [[k v] status-map]
            (if (true? v) (h1-valid! (name k) " is ok.") (h1-error! (name k) " has failed.")))
          build-exit-codes/catch-all))))

(defn run
  ([] (run []))
  ([test-aliases]
   (let [project-map (-> (build-project-map/create-project-map ".")
                         build-project-map/add-project-config
                         build-project-map/add-deps-edn)]
     (run* project-map test-aliases))))

(defn run-monorepo
  ([] (run-monorepo []))
  ([test-aliases]
   (let [monorepo-project-map (-> (build-project-map/create-project-map "")
                                  build-project-map/add-project-config
                                  build-project-map/add-deps-edn
                                  (build-apps/add-monorepo-subprojects :default)
                                  (build-apps/apply-to-subprojects
                                   build-project-map/add-project-config
                                   build-project-map/add-deps-edn))]
     (if (or (true? (get-in cli-opts [:options :generate-files]))
             (and (every? (fn [[_k v]] (not= (:status v) :fail))
                          (generate-monorepo-files/generate-files monorepo-project-map))
                  (true? (clean-state (:app-dir monorepo-project-map)))))
       (run* monorepo-project-map test-aliases)
       (do (h1-error! "State is not clean") 1)))))


(comment
  (-> (build-project-map/create-project-map "")
      build-project-map/add-project-config
      (build-apps/add-monorepo-subprojects :default)
      (build-apps/apply-to-subprojects build-project-map/add-project-config))
  ;
)
