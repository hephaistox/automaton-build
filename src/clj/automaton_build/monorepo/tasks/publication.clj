(ns automaton-build.monorepo.tasks.publication
  (:require
   [clojure.string :as str]

   [automaton-build.adapters.cfg-mgt :as cfg-mgt]
   [automaton-build.apps :as apps]
   [automaton-build.monorepo.local-test :as monorepo-lt]
   [automaton-build.monorepo.prod-release :as monorepo-release]
   [automaton-build.monorepo.local-acceptance :as monorepo-la]
   [automaton-core.adapters.log :as log]))

(defn new-feature-branch
  "Task to create a new feature branch"
  [apps {:keys [str-param1 str-param2]
         :as _task-params}]
  (let [everything-app (apps/everything apps)
        {main-branch :main-branch} everything-app
        branch-name str-param1
        base-branch str-param2]
    (cfg-mgt/create-feature-branch "."
                                   branch-name
                                   (if (str/blank? base-branch)
                                     main-branch
                                     base-branch))))

(defn ltest
  "Tasks to test the code locally"
  [apps _task-params]
  (monorepo-lt/ltest apps))

(defn la
  [apps {:keys [force? msg]
         :as _task-params}]
  (monorepo-la/la apps
                  force?
                  msg))

(defn prelease
  "Tasks publishing a customer application from the monorepo"
  [apps _task-params]
  (log/warn "Task not operational, still in progress")
  (monorepo-release/prelease apps))
