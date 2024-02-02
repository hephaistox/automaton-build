(ns automaton-build.tasks.publish-app
  (:require
   [automaton-build.cicd.deployment :as build-deployment]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files :as build-files]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Publish project as a runnable app (uber-jar) to clever cloud."
  [_task-map
   {:keys [publication environment app-dir]
    :as _app-data}]
  (let [{:keys [env]} publication
        clever-uri (get-in env [environment :clever-uri])]
    (if (build-deployment/publish-app
         clever-uri
         (->> (name environment)
              (build-files/create-dir-path app-dir "target")
              build-files/absolutize))
      build-exit-codes/ok
      build-exit-codes/catch-all)))
