#_{:heph-ignore {:forbidden-words ["tap>" "automaton-core" "automaton-web"]}}
(ns automaton-build.tasks.registry.common "Data for the common task registry")

(defn tasks
  []
  {'bg
   {:doc
    "Pause the execution - usefull for tasks that need to continue their execution in bg, like watchers"
    :la-test {:skip? true}
    :hidden? true}
   'blog {:doc "Generate the blog files"
          :mandatory-config? true
          :la-test {:skip? true}
          :pf :clj
          :build-configs [[:html-dir {:default "tmp/html/"}
                           :string]
                          [:dir {:default "blog/"}
                           :string]
                          [:pdf-dir {:default "tmp/pdf/"}
                           :string]]}
   'clean {:doc "Clean cache files for compiles, and logs"
           :build-configs [[:dirs [:vector :string]]]}
   'clean-hard
   {:doc
    "Clean all files which are not under version control (it doesn't remove untracked file or staged files if there are eligible to `git add .`)"
    :la-test {:process-opts {:in "q"}}}
   'build-jar {:doc "Compiles project to jar"
               :hidden?
               'automaton-build.tasks.registry.conditions/not-deploy-target?
               :pf :clj
               :la-test {:skip? true}
               :shared [:publication]
               :build-configs [[:aliases {:default [:run]}
                                [:vector :keyword]]
                               [:class-dir {:default "target/classes/"}
                                :string]]}
   'container-clear {:doc "Clear all local containers"
                     :la-test {:skip? true}}
   'container-list {:doc "List all available containers"}
   'docstring {:doc "Generate the documentation based on docstring"
               :mandatory-config? true
               :build-configs [[:description :string]
                               [:dir {:default "docs/code/"}
                                :string]
                               [:exclude-dirs {:default #{"resources/"}}
                                [:set :string]]
                               [:title :string]]
               :pf :clj}
   'error {:doc "Run intentionaly an error."
           :la-test {:expected-exit-code 131}
           :hidden? true}
   'error-clj {:doc "Run intentionaly an error on clj."
               :la-test {:expected-exit-code 131}
               :pf :clj
               :hidden? true}
   'format-code {:doc "Format the whole documentation"
                 :build-configs [[:include-files {:default #{"build_config.edn"
                                                             "deps.edn"
                                                             "shadow-cljs.edn"}}
                                  [:set :string]]]}
   'gha-container-publish {:doc "Update the gha container to run that app"
                           :hidden?
                           'automaton-build.tasks.registry.conditions/not-cicd?
                           :la-test {:skip? true}
                           :shared [:gha :account]
                           :task-cli-opts-kws [:tag]}
   'gha-lconnect {:doc "Connect to a local container running this code"
                  :shared [:gha :account]
                  :hidden? 'automaton-build.tasks.registry.conditions/not-cicd?
                  :la-test {:skip? true}}
   'is-cicd {:doc "Tested if runned on cicd"
             :la-test {:cmd ["bb" "heph-task" "is-cicd" "-f"]}
             :hidden? true
             :task-cli-opts-kws [:force]}
   'la {:doc "Local acceptance test"
        :la-test {:skip? true}}
   'lbe-repl
   {:doc
    "Connect to repl - this command is to be used by workflow, a version apart from build_app is directly set in `bb.edn`."
    :build-configs [[:repl-aliases {:default [:common-test
                                              :env-development-repl]}
                     [:vector :keyword]]]
    :la-test {:skip? true}
    :hidden? true}
   'lbe-test {:doc "Local Backend test"
              :la-test {:skip? true}
              :build-configs [[:test-aliases {:default [:env-development-test
                                                        :common-test]}
                               [:vector :keyword]]]}
   'lfe-watch
   {:doc
    "Compile local modifications for development environment and watch the modifications"
    :mandatory-config? true
    :la-test {:skip? true}
    :shared [:publication]}
   'lfe-test {:doc "Local frontend test"
              :la-test {:skip? true}
              :mandatory-config? true}
   'lint {:doc "Apply linter on project source code."}
   'mermaid {:doc "Build all mermaid files"
             :shared [:mermaid-dir]}
   'mermaid-watch {:doc "Watch mermaid files modifications"
                   :shared [:mermaid-dir]
                   :la-test {:skip? true}}
   'publish-library
   {:doc
    "Publish project, by deploying the jar to clojars and pushing git main branch with new version"
    :pf :clj
    :shared [:publication]
    :la-test {:skip? true}}
   'publish-app
   {:doc
    "Publish project to CC, same as above, but instead of clojars the uberjar is pushed to clever-cloud repo."
    :pf :clj
    :shared [:publication]
    :task-cli-opts-kws [:environment]
    :la-test {:skip? true}}
   'push-local {:doc "Push this repo"
                :la-test {:skip? true}
                :shared [:publication]
                :task-cli-opts-kws [:force :message :tag :environment]
                :pf :clj}
   'reports
   {:doc "Creates the reports of code analysis"
    :build-configs
    [[:alias-outputfilename {:default "docs/code/alias.edn"}
      :string]
     [:comments-outputfilename {:default "docs/code/comments.edn"}
      :string]
     [:forbiddenwords-words {:default #{"tap>"}}
      [:set :string]]
     [:forbiddenwords-outputfilename {:default "docs/code/forbbiden-words.edn"}
      :string]
     [:shadow-report-outputfilename {:default "doc/codes/code-size.edn"}
      :string]
     [:shadow-report-app {:default :app}
      :keyword]
     [:css-outputfilename {:default "docs/code/css.edn"}
      :string]
     [:namespace-outputfilename {:default "docs/code/namespace.edn"}
      :string]
     [:stats-outputfilename {:default "docs/code/stats.md"}
      :string]]}
   'storage-install
   {:doc "Install a datomic local engine"
    :shared [:storage-datomic]
    :la-test {:skip? true}
    :task-cli-opts-kws [:force]
    :build-configs
    [[:datomic-url-pattern
      {:default
       "https://datomic-pro-downloads.s3.amazonaws.com/%1$s/datomic-pro-%1$s.zip"}
      :string]]}
   'storage-start {:doc "Starts a datomic transactor."
                   :shared [:storage-datomic]
                   :hidden? true}
   'tasks {:doc "List all tasks."
           :shared [:publication]}
   'update-deps
   {:doc
    "Update the dependencies, cider-nrepl and refactor are to be updated manually"
    :build-configs [[:exclude-libs
                     {:default #{"cider/cider-nrepl"
                                 "org.clojars.hephaistox/automaton-build@*-*"
                                 "org.clojars.hephaistox/automaton-core@*-*"
                                 "org.clojars.hephaistox/automaton-web@*-*"
                                 "org.clojars.hephaistox/automaton-web-dev@*-*"
                                 "com.taoensso/encore"
                                 "refactor-nrepl/refactor-nrepl"
                                 "com.github.liquidz/antq"}}
                     [:set :string]]]
    :la-test {:skip? true}
    :pf :clj}
   'visualize-deps {:doc "Visualize the dependencies in a graph"
                    :build-configs [[:output-file {:default
                                                   "docs/code/deps.svg"}
                                     :string]]
                    :hidden? true
                    :pf :clj}
   'visualize-ns {:doc "Visualize the namespaces in graph"
                  :hidden? true
                  :build-configs [[:output-file {:default
                                                 "docs/code/deps-ns.svg"}
                                   :string]]
                  :pf :clj}
   'wf-2 {:doc "Start repls"
          :group :wf
          :step 2
          :wk-tasks ['storage-start 'lbe-repl 'lfe-watch 'bg]}
   'wf-3 {:doc "Quick verifications and formatting for IDE usage"
          :group :wf
          :step 3
          :wk-tasks ['reports 'format-code 'lint]}
   'wf-3f {:doc "Full work verification workflow (this task is temporary)"
           :group :wf
           :step 3
           :wk-tasks ['reports 'format-code 'lint 'lbe-test 'lfe-test]}
   'wf-6 {:doc
          "Push the local version - create gha docker image - push to the repo"
          :group :wf
          :step 6
          :wk-tasks ['clean
                     'lint
                     'lbe-test
                     'lfe-test
                     'reports
                     'blog
                     'mermaid
                     #_'visualize-deps
                     'visualize-ns
                     'format-code
                     'gha-container-publish
                     'push-local-dir-to-repo]}
   'gha {:doc "Github action tests - launched is automatically by github"
         :group :gha
         :la-test {:cmd ["bb" "heph-task" "gha" "-f"]}
         :step 1
         :wk-tasks ['is-cicd 'lint 'lbe-test]}})
