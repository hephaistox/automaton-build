(ns automaton-build.monorepo.tasks.apps
  "Monorepo tasks related to apps"
  (:require
   [automaton-core.adapters.log :as log]
   [automaton-build.container.core :as container-core]
   [automaton-build.apps :as apps]))

(defn apps
  "Print names of the apps"
  [apps _task-params]
  (log/info "List is" (apps/app-names apps))
  (log/info "Apps with documentation: " (apps/app-but-everything-names apps))
  (log/info "Customer apps are " (apps/cust-app-names apps))
  (log/info "Container image container name: " (container-core/container-image-container-names))
  (log/info "Everything " (:app-name  (apps/everything apps)))
  (log/info "Template app is " (:app-name (apps/template-app apps))))
