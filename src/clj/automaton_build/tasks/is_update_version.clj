(ns automaton-build.tasks.is-update-version
  "Tells if project should have an version update"
  (:require
   [automaton-build.cicd.cfg-mgt  :as build-cfg-mgt]
   [automaton-build.cicd.version  :as build-version]
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.utils.keyword :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map
   {:keys [app-dir app-name environment publication]
    :as _app}]
  (build-log/info-format "Checking if %s should have a version update" app-name)
  (let [environment (-> environment
                        build-utils-keyword/trim-colon
                        build-utils-keyword/keywordize)
        current-version (build-version/current-version app-dir)
        prod? (build-version/production? current-version)]
    (if (and (:repo publication)
             (build-cfg-mgt/changed?
              app-dir
              (:repo publication)
              (get-in publication
                      [:env (if prod? :production environment) :push-branch])))
      build-exit-codes/ok
      build-exit-codes/catch-all)))
