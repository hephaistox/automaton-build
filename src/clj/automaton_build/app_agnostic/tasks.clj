(ns automaton-build.app-agnostic.tasks
  "The tasks agnostic from applications"
  (:require
   [automaton-build.adapters.log :as log]
   [automaton-build.app-agnostic.tasks.clean :as clean-tasks]
   [automaton-build.app-agnostic.tasks.container :as container-tasks]
   [automaton-build.exit-codes :as exit-codes]))

(def tasks
  [["clean-hard" {:cli-params-mode :none
                  :doc "Clean as the repository is freshly cloned"
                  :exec-task clean-tasks/clean-hard}]

   ["container-clean" {:cli-params-mode :none
                       :doc "Remove all container images"
                       :exec-task container-tasks/container-clean}]

   ["container-image-list" {:cli-params-mode :none
                            :doc "List container images"
                            :exec-task container-tasks/container-image-list}]

   ["error" {:cli-params-mode :none
             :doc "Trigger an error"
             :exec-task (fn [_ _]
                          (log/fatal "This error is raised intentionally")
                          (System/exit exit-codes/intentional))}]])
