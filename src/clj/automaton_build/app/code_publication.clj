(ns automaton-build.app.code-publication
  (:require
   [automaton-build.adapters.log :as log]
   [automaton-build.adapters.files :as files]
   [automaton-build.adapters.cfg-mgt :as cfg-mgt]
   [automaton-build.env-setup :as env-setup]))

(defn assembly-app-dir
  "Directory where the publication of the application occur"
  [app-name]
  (files/create-dir-path (get-in env-setup/env-setup [:published-apps :dir])
                         app-name))

(defn- push*
  "Publish the application"
  [app-dir assembly-dir repo-address branch current-feature-branch commit-msg]
  (cfg-mgt/clone-repo assembly-dir repo-address branch)
  (cfg-mgt/change-local-branch assembly-dir current-feature-branch)
  (cfg-mgt/clean-to-repo assembly-dir false)
  (files/copy-files-or-dir [app-dir]
                           assembly-dir)

  (if (cfg-mgt/is-working-tree-clean? assembly-dir true)
    (log/warn "No change to be pushed")
    (cfg-mgt/push-repo assembly-dir
                       commit-msg)))

(defn push
  "Publish the application `app`
  Return the last `commit-id`, even if no changes occur
  * `app` is the application to push
  * `commit-msg` the message of the commit"
  [{:keys [app-name app-dir publication]
    :as _app}                             commit-msg]
  (let [assembly-dir (files/create-dir-path (assembly-app-dir app-name)
                                            (get-in env-setup/env-setup [:published-apps :code-subdir]))
        {:keys [repo-address branch]} publication
        current-feature-branch (cfg-mgt/current-branch ".")]
    (log/info "Publish app `" app-name "` in directory `" assembly-dir "`")
    (if (some nil? [repo-address branch])
      (log/warn "Project " app-name " could not be published, some publication data are missing"
                {:publication publication})
      (push* app-dir assembly-dir repo-address branch current-feature-branch commit-msg))
    (let [commit-id (cfg-mgt/get-commit-id assembly-dir)]
      (log/debug "git log found commit id " commit-id)
      commit-id)))
