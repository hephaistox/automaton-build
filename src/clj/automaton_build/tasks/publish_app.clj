(ns automaton-build.tasks.publish-app
  (:require
   [automaton-build.cicd.deployment :as build-deployment]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.configuration :as build-conf]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  "Publish project as a runnable app (uber-jar) to clever cloud."
  [_task-map
   {:keys [publication]
    :as _app-data}]
  (let [{:keys [env]} publication
        current-env (build-conf/read-param [:env])
        clever-uri (get-in env [current-env :clever-uri])]
    (if (build-deployment/publish-app clever-uri
                                      (str "target/" (name current-env)))
      build-exit-codes/ok
      build-exit-codes/catch-all)))
