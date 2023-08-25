(ns automaton-build.monorepo.tasks.hosting
  "Gather monorepo to call hosting related tasks"
  (:require
   [automaton-build.adapters.hosting :as hosting]))

(defn pconnect
  "Task to connect to the production server"
  [_apps _task-params]
  (hosting/prod-ssh "."))
