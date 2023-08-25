(ns automaton-build.monorepo.tasks.template
  "Monorepo tasks related to template"
  (:require
   [automaton-build.adapters.cfg-mgt :as cfg-mgt]
   [automaton-build.apps :as apps]
   [automaton-build.apps.templating :as template]))

(defn new-project
  "Tasks to create a project"
  [apps {:keys [cust-app-name]
         :as _task-params}]
  (when-not (cfg-mgt/is-working-tree-clean? ".")
    (throw (ex-info "Please clean your configuration management working tree before refreshing"
                    {:dir "."})))
  (template/new-project (:app-dir (apps/template-app apps))
                        cust-app-name))
