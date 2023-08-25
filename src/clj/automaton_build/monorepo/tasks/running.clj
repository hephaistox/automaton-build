(ns automaton-build.monorepo.tasks.running
  "Running application in the monorepo"
  (:require
   [automaton-build.app.running :as running]
   [automaton-build.apps :as apps]))

(def monorepo-repl-alias
  "everything")

(defn plrun
  "Launch everything for the dev environment for an application"
  [apps
   {:keys [cust-app-name]
    :as _task-params}]
  (let [app (apps/search-app-by-name apps cust-app-name)]
    (running/run-prepl app
                       monorepo-repl-alias)
    (running/watch-cljs app)))

(defn pprun
  "Run the project in production mode"
  [apps
   {:keys [cust-app-name]
    :as _task-params}]
  (let [app (apps/search-app-by-name apps cust-app-name)]
    (running/run-prod-be app)))
