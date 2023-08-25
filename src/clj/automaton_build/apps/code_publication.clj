(ns automaton-build.apps.code-publication
  (:require
   [automaton-build.app :as app]
   [automaton-build.adapters.log :as log]
   [automaton-build.app.code-publication :as app-code-pub]))

(defn push-a-lib
  "Push a library to its repo. All `app` in `apps` are scanned, all deps edn are updated, except the everything and template applications.
  Params:
  * `lib-app` is the library application
  * `apps` applications
  * `commit-msg` the commit message"
  [{:keys [app-name] :as lib-app} apps commit-msg]
  (log/info "Push Hephaistox library ` " app-name "` to its repo")
  (let [commit-id (app-code-pub/push lib-app
                                     commit-msg)]
    (doseq [app apps]
      (when-not (or (:everything? app)
                    (:template-app? app))
        (app/update-project-deps app
                                 lib-app
                                 commit-id)))))
