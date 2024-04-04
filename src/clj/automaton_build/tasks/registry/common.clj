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
    :task-cli-opts-kws [:force]
    :la-test {:process-opts {:in "q"}}}
   'commit {:doc "Commit and push, disallowed for production branch."
            :hidden? true
            :task-cli-opts-kws [:message-opt]
            :la-test {:skip? true}}
   'build-jar {:doc "Compiles project to jar"
               :hidden?
               'automaton-build.tasks.registry.conditions/not-deploy-target?
               :pf :clj
               :la-test {:skip? true}
               :shared [:publication]
               :task-cli-opts-kws [:environment]}
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
   'format-code {:doc "Format the whole documentation"}
   'generate-code-stats {:doc "Update code statistics"
                         :la-test {:skip? true}
                         :build-configs [[:stats-outputfilename
                                          {:default "docs/code/stats.md"}
                                          :string]]}
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
   'generate-pom-xml {:doc "Generate pom xml file in root of an app"
                      :pf :clj
                      :shared [:publication]
                      :task-cli-opts-kws [:environment :force]
                      :la-test {:skip? true}}
   'is-cicd {:doc "Tested if runned on cicd"
             :la-test {:cmd ["bb" "heph-task" "is-cicd" "-f"]}
             :hidden? true
             :task-cli-opts-kws [:force]}
   'la {:doc "Local acceptance test"
        :la-test {:skip? true}}
   'la-without-opts {:doc
                     "Local acceptance test without any opts passed to a task"
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
   'lfe-css {:doc "Compile css and watch for modifications"
             :mandatory-config? true
             :la-test {:skip? true}
             :shared [:publication]}
   'lfe-test {:doc "Local frontend test"
              :la-test {:skip? true}
              :mandatory-config? true}
   'lfe-manual {:doc "Asks user if tests are passing"
                :task-cli-opts-kws [:force]
                :la-test {:skip? true}}
   'lint {:doc "Apply linter on project source code."}
   'mermaid {:doc "Build all mermaid files"}
   'mermaid-watch {:doc "Watch mermaid files modifications"
                   :la-test {:skip? true}}
   'deploy {:doc "Compile, deploy to gha, push to base branch and publish jar."
            :pf :clj
            :la-test {:skip? true}
            :shared [:publication :gha :account]
            :task-cli-opts-kws [:environment :force]}
   'publish-jar
   {:doc
    "Publish project, by deploying the jar to either clojars or clever cloud"
    :pf :clj
    :shared [:publication]
    :task-cli-opts-kws [:environment]
    :la-test {:skip? true}}
   'git-push-local-branch {:doc "Push this repo"
                           :la-test {:skip? true}
                           :shared [:publication]
                           :task-cli-opts-kws [:force :message-opt :environment]
                           :pf :clj}
   'git-push-base-branch {:doc "Push this repo to base branch of environment"
                          :la-test {:skip? true}
                          :shared [:publication]
                          :task-cli-opts-kws [:force :message :environment]
                          :pf :clj}
   'pull-base-branch {:doc "Checks if you are up to date with base branch."
                      :pf :clj
                      :shared [:publication]
                      :la-test {:skip? true}}
   'clean-state {:doc "Checks if you are in a clean state in terms of changes."
                 :la-test {:skip? true}}
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
    :build-configs [[:exclude-libs {:optional true}
                     [:set :string]]]
    :la-test {:skip? true}
    :pf :clj}
   'update-gha-workflow-file {:doc "Update gha workflow file of an app"
                              :pf :clj
                              :shared [:gha :account]
                              :task-cli-opts-kws [:environment]
                              :la-test {:skip? true}}
   'update-version {:doc "Update version"
                    :pf :clj
                    :task-cli-opts-kws [:environment]
                    :la-test {:skip? true}}
   'visualize-deps {:doc "Visualize the dependencies in a graph"
                    :build-configs [[:output-file {:default
                                                   "docs/code/deps.svg"}
                                     :string]]
                    :hidden? true
                    :pf :clj}
   'visualize-ns {:doc "Visualize the namespaces in graph"
                  :hidden? true
                  :la-test {:skip? true}
                  :build-configs [[:output-file {:default
                                                 "docs/code/deps-ns.svg"}
                                   :string]]
                  :pf :clj}
   'wf-2 {:doc "Start repls"
          :group :wf
          :step 2
          :wk-tasks ['lbe-repl 'lfe-css 'lfe-watch 'mermaid-watch]}
   'wf-3 {:doc "Quick verifications and formatting for IDE usage"
          :group :wf
          :step 3
          :wk-tasks ['reports 'format-code 'lint 'commit]}
   'wf-3f {:doc "Full work verification workflow"
           :group :wf
           :step 3
           :wk-tasks
           ['reports 'format-code 'lint 'lbe-test 'lfe-manual 'commit]}
   'gha {:doc "Github action tests - launched is automatically by github"
         :group :gha
         :la-test {:cmd ["bb" "heph-task" "gha" "-f"]}
         :step 1
         :wk-tasks ['is-cicd 'lint 'lbe-test]}})
