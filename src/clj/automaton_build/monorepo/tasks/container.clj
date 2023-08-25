(ns automaton-build.monorepo.tasks.container
  "Tasks to manage monorepo container"
  (:require
   [automaton-build.apps :as apps]
   [automaton-build.container.clever-cloud :as cc-container]
   [automaton-build.container.github-action :as gha-container]))

(defn lconnect
  "Task to connect to a local cust-app image"
  [apps {:keys [cust-app-name] :as _task-params}]
  (let [app (apps/search-app-by-name apps
                                     cust-app-name)
        {:keys [app-dir]} app]
    (cc-container/connect cust-app-name
                          app-dir
                          "..")))

(defn gha-connect
  "Task to connect to one of the local container image"
  [apps {:keys [app-name] :as _task-params}]
  (let [app (apps/search-app-by-name apps
                                     app-name)
        {:keys [app-dir]} app]
    (gha-container/connect app-name
                           app-dir
                           "..")))
