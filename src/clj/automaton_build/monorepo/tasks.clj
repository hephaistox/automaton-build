(ns automaton-build.monorepo.tasks
  "Gather all tasks definition for monorepo bb cli.

  The `:cli-params-mode` is reffering the option as in `build.cli-params/validate`"
  (:require
   [automaton-build.app-agnostic.tasks :as app-agnostic-tasks]
   [automaton-build.monorepo.tasks.apps :as api-apps]
   [automaton-build.monorepo.tasks.blog :as bmtb]
   [automaton-build.monorepo.tasks.code-helpers :as code-helpers-tasks]
   [automaton-build.monorepo.tasks.clean :as mono-clean-tasks]
   [automaton-build.monorepo.tasks.container :as container-tasks]
   [automaton-build.monorepo.tasks.hosting :as hosting-tasks]
   [automaton-build.monorepo.tasks.publication :as publication-tasks]
   [automaton-build.monorepo.tasks.running :as running-tasks]
   [automaton-build.monorepo.tasks.template :as template-tasks]))

(def tasks
  "Define tasks for monorepo project, standard tasks are available also but could be overriden"
  (vec (concat app-agnostic-tasks/tasks
               [["apps" {:cli-params-mode :none
                         :doc "List all applications"
                         :create-deps-at-startup? true
                         :exec-task api-apps/apps}]

                ["blog-md-pdf" {:cli-params-mode :file-name-and-language
                                :doc "Transforms md file to pdf based on edn file."
                                :exec-task bmtb/blog-md->pdf}]

                ["clean" {:cli-params-mode :none
                          :doc "Clean the temporary files"
                          :create-deps-at-startup? true
                          :exec-task mono-clean-tasks/clean}]

                ["gha" {:cli-params-mode :force?
                        :doc "To be executed on github. Use `bb gha force?` if you want to workaround that check"
                        :create-deps-at-startup? true
                        :exec-task code-helpers-tasks/gha}]

                ["gha-connect" {:cli-params-mode :one-app
                                :doc "Connect to github action test container image"
                                :create-deps-at-startup? true
                                :exec-task container-tasks/gha-connect}]

                ["la" {:cli-params-mode :msg-force?
                       :doc "Test local acceptance"
                       :create-deps-at-startup? true
                       :exec-task publication-tasks/la}]

                ["ltest" {:cli-params-mode :none
                          :doc "Test locally the monorepo"
                          :create-deps-at-startup? true
                          :exec-task publication-tasks/ltest}]

                ["new-branch" {:cli-params-mode :string-optstring
                               :doc "Create a new branch, on a refreshed base branch"
                               :create-deps-at-startup? true
                               :exec-task publication-tasks/new-feature-branch}]

                ["new-project" {:cli-params-mode :one-not-app-names
                                :doc "Workflow feature 0 - creates a new project"
                                :create-deps-at-startup? true
                                :exec-task template-tasks/new-project}]

                ["outdated" {:cli-params-mode :none
                             :doc "Are there some outdated libs?"
                             :create-deps-at-startup? true
                             :exec-task code-helpers-tasks/outdated}]

                ["plconnect" {:cli-params-mode :one-cust-app
                              :doc "For a cust-app, connect ssh to its local environment, (in the container image)"
                              :create-deps-at-startup? true
                              :exec-task container-tasks/lconnect}]

                ["prelease" {:cli-params-mode :one-cust-app
                             :doc "Monorepo, release a customer application"
                             :create-deps-at-startup? true
                             :exec-task publication-tasks/prelease}]

                ["plrun" {:cli-params-mode :one-cust-app
                          :doc "For a cust-app, launch both back and frontend in local environment, dev mode"
                          :create-deps-at-startup? true
                          :exec-task running-tasks/plrun}]

                ["ppconnect" {:cli-params-mode :one-cust-app
                              :doc "For a cust-app, connect to the production environment"
                              :create-deps-at-startup? true
                              :exec-task hosting-tasks/pconnect}]

                ["pprun" {:cli-params-mode :one-cust-app
                          :doc "For a cust-app, launch the app in production mode, prod mode"
                          :create-deps-at-startup? true
                          :exec-task running-tasks/pprun}]])))
