(ns automaton-build.tasks.clean-hard
  (:require
   [automaton-build.cicd.cfg-mgt  :as build-cfg-mgt]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files      :as build-files]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Clean the repository to the state as it's after being cloned from git server"
  [_task-map {:keys [app-dir force]}]
  (let [clean-res (-> (build-files/absolutize app-dir)
                      (build-cfg-mgt/clean-hard (not force)))]
    (if clean-res build-exit-codes/ok build-exit-codes/catch-all)))
