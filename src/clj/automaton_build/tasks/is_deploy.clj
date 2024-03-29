(ns automaton-build.tasks.is-deploy
  "Tells if project should be deployed"
  (:require
   [automaton-build.cicd.cfg-mgt  :as build-cfg-mgt]
   [automaton-build.cicd.version  :as build-version]
   [automaton-build.log           :as build-log]
   [automaton-build.os.exit-codes :as build-exit-codes]
   [automaton-build.os.files      :as build-files]
   [automaton-build.utils.keyword :as build-utils-keyword]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn exec
  [_task-map
   {:keys [app-dir app-name environment publication]
    :as _app}]
  (build-log/info-format "Checking if %s should be deployed" app-name)
  (if-let [repo (:repo publication)]
    (let [environment (-> environment
                          build-utils-keyword/trim-colon
                          build-utils-keyword/keywordize)
          tmp-dir (build-files/create-temp-dir)]
      (build-cfg-mgt/clone-file repo
                                app-name
                                tmp-dir
                                (get-in publication
                                        [:env environment :push-branch])
                                (build-version/version-file))
      (if (= (build-version/current-version
              (build-files/create-dir-path tmp-dir app-name))
             (build-version/current-version app-dir))
        build-exit-codes/catch-all
        build-exit-codes/ok))
    (do (build-log/debug-format "`%s` does not have a git repo so it's skipped"
                                app-name)
        build-exit-codes/catch-all)))
