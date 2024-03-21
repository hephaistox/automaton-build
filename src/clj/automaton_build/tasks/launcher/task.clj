(ns automaton-build.tasks.launcher.task
  "The task to be launched

  Design decision:
  * This task is the one ready to launch, which is not the task in the registry"
  (:require
   [automaton-build.tasks.registry      :as build-task-registry]
   [automaton-build.tasks.registry.find :as build-task-registry-find]))

(defn build
  "Prepare tasks data:
  * `task-map` for the details of the task setup
  * `task-cli-opts` for the cli options applied to the task
  * `task-registry` registry of tasks
  * `setuped-tasks` list of tasks"
  [app-dir task-name setuped-tasks]
  (let [task-registry (build-task-registry/build app-dir setuped-tasks)
        tasks-schema (build-task-registry/build-config-schema task-registry)
        task-map (build-task-registry-find/task-map task-registry task-name)]
    {:task-map task-map
     :tasks-schema tasks-schema
     :task-registry task-registry
     :mandatory-tasks (-> task-registry
                          build-task-registry/not-mandatory
                          build-task-registry/task-names)}))
