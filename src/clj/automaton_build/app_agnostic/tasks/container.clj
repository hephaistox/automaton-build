(ns automaton-build.app-agnostic.tasks.container
  "Tasks to manage application containers"
  (:require
   [automaton-core.adapters.log :as log]

   [automaton-build.adapters.container :as container]))

(defn container-image-list
  "Task to list all images"
  [_apps _task-params]
  (container/container-installed?)
  (log/trace (str "\n" (container/container-image-list))))

(defn container-clean
  "Task to clean containers and images"
  [_apps _task-params]
  (container/container-installed?)
  (container/container-clean))
