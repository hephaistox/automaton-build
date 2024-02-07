(ns automaton-build.tasks.launcher.launch-on-same-env
  (:require
   [automaton-build.utils.namespace :as build-namespace]))

(defn same-env
  "Run the `body-fn` on the current environment
  If the task is started directly from bb, it will continue like that
  If the task is launched from another clj task, it will continue on clj"
  [task-map app-data]
  (build-namespace/symbol-to-fn-call (:task-fn task-map) task-map app-data))
