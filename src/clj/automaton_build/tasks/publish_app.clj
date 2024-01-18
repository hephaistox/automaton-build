(ns automaton-build.tasks.publish-app
  (:require
   [automaton-build.cicd.deployment :as build-deployment]
   [automaton-build.os.exit-codes :as build-exit-codes]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Publish project as a runnable app (uber-jar) to clever cloud."
  [_task-map
   {:keys [publication]
    :as _app-data}]
  (let [{:keys [clever-uri]} publication]
    (if (build-deployment/publish-app clever-uri)
      build-exit-codes/ok
      build-exit-codes/catch-all)))
