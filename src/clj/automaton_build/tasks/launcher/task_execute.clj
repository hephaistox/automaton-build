(ns automaton-build.tasks.launcher.task-execute
  (:require
   [automaton-build.log :as build-log]
   [automaton-build.log.files :as build-log-files]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.tasks.launcher.app-data :as build-tasks-app-data]
   [automaton-build.tasks.launcher.pf-dispatcher :as build-pf-dispatcher]))

(defn task-execute
  "Execute the task `task-name` with arguments `cli-args` in the application `app`."
  [app-dir task-name cli-args]
  (let [{:keys [task-map cli-opts]
         :as app-data}
        (build-tasks-app-data/task-app-data app-dir task-name cli-args)]
    (build-log-files/save-debug-info "app_data.edn" app-data "For debug only.")
    (cond
      (nil? app-data) (do (build-log/error-format "No data found for task `%s`"
                                                  task-name)
                          build-exit-codes/cannot-execute)
      (some? task-map)
      (or (build-pf-dispatcher/dispatch task-map app-data cli-opts)
          build-exit-codes/catch-all)
      :else (do (build-log/error-format "The task `%s` is unknown" task-name)
                build-exit-codes/invalid-argument))))
