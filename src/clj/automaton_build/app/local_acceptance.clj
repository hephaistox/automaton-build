(ns automaton-build.app.local-acceptance
  (:require
   [automaton-build.adapters.cfg-mgt :as cfg-mgt]
   [automaton-build.adapters.edn-utils :as edn-utils]
   [automaton-build.adapters.log :as log]
   [automaton-build.app.doc :as app-doc]
   [automaton-build.app.code-publication :as app-pub]
   [automaton-build.container.clever-cloud :as cc-container]
   [automaton-build.container.github-action :as gha-container]))

(defn la
  "Local acceptance for one application
  Params:
  * `app` application
  * `commit-msg` commit message
  * `force-local-modifications?` (Optional, default = false) if true, push even if there are local modifications in the working directory"
  ([app commit-msg]
   (la app commit-msg false))
  ([{:keys [app-name app-dir run-env publication] :as app} commit-msg force-local-modifications?]
   (when (and (not force-local-modifications?)
              (not (cfg-mgt/is-working-tree-clean? ".")))
     (throw (ex-info "The working tree should be clean to start" {})))

   (app-doc/size-optimization-report app)

   (when run-env
     (cc-container/build app-name
                         app-dir)

     (gha-container/build app-name
                          app-dir))

   (when publication
     (app-pub/push app
                   commit-msg))
   #_(pub-app/run-test-env app))
  )

(defn run-test-env
  "Run a test environement to that application
  * `app` is the application we want to push"
  [{:keys [app-name] :as app}]
  (let [test-env (get-in app [:run-env :la-env])
        {:keys [host-repo-address]} test-env]
    (if host-repo-address
      (log/debug "Push ` " app-name "`to the test environment @ `" host-repo-address "`")
      #_(cfg-mgt/clone-repo app)

      (do
        (log/warn "Application `" app-name "` won't be tested as its parameters are not valid")
        (log/trace (edn-utils/spit-in-tmp-file test-env))))))
