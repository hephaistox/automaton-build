(ns automaton-build.app-agnostic.tasks.clean
  "Clean to repository"
  (:require
   [automaton-build.adapters.cfg-mgt :as cfg-mgt]
   [automaton-core.adapters.files :as files]
   [automaton-core.adapters.log :as log]))

(defn clean-hard
  "Clean the repository to the state as it's after being cloned from git server"
  [_apps _task-params]
  (let [dir (files/absolutize ".")]
    (log/info "Clean to repo on directory " dir)
    (cfg-mgt/clean-to-repo dir)))
